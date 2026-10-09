// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.SystemClock
import io.github.dante_souza.cuicatl.domain.MeasurementFrame
import io.github.dante_souza.cuicatl.domain.MeasurementMath
import io.github.dante_souza.cuicatl.domain.QualityFlag
import io.github.dante_souza.cuicatl.domain.SignalState
import io.github.dante_souza.cuicatl.domain.TimingQuality
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.log10

data class CaptureConfiguration(
    val source: String,
    val sampleRateHz: Int,
    val inputIdentity: String,
    val processingState: String,
)

class AndroidSessionCapture(
    context: Context,
    private val sessionId: String,
    private val onStarted: (CaptureConfiguration) -> Unit,
    private val onFrame: (MeasurementFrame) -> Unit,
    private val onStopped: (elapsedMillis: Long) -> Unit,
    private val onFailure: (error: Throwable, elapsedMillis: Long) -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    @Volatile
    private var activeRecord: AudioRecord? = null

    @Volatile
    private var startedAtElapsedRealtime: Long = 0L

    fun start() {
        if (!running.compareAndSet(false, true)) return
        stopRequested.set(false)
        thread(name = "cuicatl-phase1-capture", isDaemon = true) {
            runCapture()
        }
    }

    fun stop() {
        stopRequested.set(true)
        running.set(false)
        val record = activeRecord
        if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            runCatching { record.stop() }
        }
    }

    override fun close() {
        stop()
    }

    @SuppressLint("MissingPermission")
    private fun runCapture() {
        var record: AudioRecord? = null
        var sequence = 0L
        var intervalStartSample = 0L
        var intervalSampleCount = 0
        var intervalSumSquares = 0.0
        var intervalPeak = 0.0
        var intervalClipped = 0

        try {
            val selection = openFirstSupportedRecord()
                ?: error("No supported mono PCM16 source/rate configuration initialized")

            val active = selection.record
            record = active
            activeRecord = active
            active.startRecording()
            if (active.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                error("AudioRecord did not enter RECORDSTATE_RECORDING")
            }

            startedAtElapsedRealtime = SystemClock.elapsedRealtime()
            val rate = active.sampleRate
            val intervalSamples = maxOf(1, rate / 10)
            val buffer = ShortArray(maxOf(1024, intervalSamples))
            var expectedRouteId = active.routedDevice?.id
            var expectedRouteIdentity = describeRoute(active.routedDevice)
            val configuration = CaptureConfiguration(
                source = sourceName(active.audioSource),
                sampleRateHz = rate,
                inputIdentity = expectedRouteIdentity,
                processingState = processingAvailability(),
            )
            onStarted(configuration)

            fun ensureRouteUnchanged() {
                val routedDevice = active.routedDevice ?: return
                if (expectedRouteId == null) {
                    expectedRouteId = routedDevice.id
                    expectedRouteIdentity = describeRoute(routedDevice)
                    return
                }
                if (routedDevice.id != expectedRouteId) {
                    error(
                        "Input route changed during capture: " +
                            expectedRouteIdentity + " -> " + describeRoute(routedDevice),
                    )
                }
            }

            fun emitFrame() {
                if (intervalSampleCount <= 0) return

                val meanSquare = intervalSumSquares / intervalSampleCount
                val rms = MeasurementMath.rmsFromMeanSquare(meanSquare)
                val rmsDbfs = MeasurementMath.dbfsFromMeanSquare(meanSquare)
                val peakDbfs =
                    if (intervalPeak == 0.0) null else 20.0 * log10(intervalPeak)
                val durationMillis =
                    (intervalSampleCount.toLong() * 1000L) / rate.toLong()
                val elapsedStartMillis =
                    (intervalStartSample * 1000L) / rate.toLong()
                val qualityFlags =
                    if (intervalClipped > 0) setOf(QualityFlag.CLIPPED) else emptySet()

                onFrame(
                    MeasurementFrame(
                        sessionId = sessionId,
                        sequence = sequence,
                        elapsedStartMillis = elapsedStartMillis,
                        durationMillis = durationMillis,
                        sampleRateHz = rate,
                        sampleCount = intervalSampleCount,
                        meanSquareFs = meanSquare,
                        rmsFs = rms,
                        samplePeakFs = intervalPeak,
                        rmsDbfs = rmsDbfs,
                        samplePeakDbfs = peakDbfs,
                        signalState =
                            if (meanSquare == 0.0) SignalState.DIGITAL_ZERO
                            else SignalState.NONZERO,
                        timingQuality = timingQuality(active),
                        clippedSampleCount = intervalClipped,
                        qualityFlags = qualityFlags,
                    ),
                )

                sequence += 1
                intervalStartSample += intervalSampleCount
                intervalSampleCount = 0
                intervalSumSquares = 0.0
                intervalPeak = 0.0
                intervalClipped = 0
            }

            while (running.get()) {
                val read = active.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read < 0) {
                    if (!running.get() || stopRequested.get()) break
                    error("AudioRecord.read failed with code " + read)
                }
                if (read == 0) continue

                // A Phase 1 session has one fixed input configuration.
                // Reject a detected route change before consuming the newly read block.
                ensureRouteUnchanged()

                for (index in 0 until read) {
                    val raw = buffer[index]
                    val normalized = raw.toDouble() / 32768.0
                    intervalSumSquares += normalized * normalized
                    intervalPeak = maxOf(intervalPeak, abs(normalized))
                    if (raw == Short.MIN_VALUE || raw == Short.MAX_VALUE) {
                        intervalClipped += 1
                    }
                    intervalSampleCount += 1

                    if (intervalSampleCount == intervalSamples) {
                        emitFrame()
                    }
                }
            }

            if (intervalSampleCount > 0) {
                emitFrame()
            }

            val elapsed = elapsedSinceStart()
            onStopped(elapsed)
        } catch (error: Throwable) {
            if (stopRequested.get()) {
                onStopped(elapsedSinceStart())
            } else {
                onFailure(error, elapsedSinceStart())
            }
        } finally {
            running.set(false)
            activeRecord = null
            if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                runCatching { record.stop() }
            }
            runCatching { record?.release() }
        }
    }

    private fun elapsedSinceStart(): Long =
        if (startedAtElapsedRealtime == 0L) 0L
        else SystemClock.elapsedRealtime() - startedAtElapsedRealtime

    @SuppressLint("MissingPermission")
    private fun openFirstSupportedRecord(): Selection? {
        val audioManager = appContext.getSystemService(AudioManager::class.java)
        val unprocessedAdvertised =
            audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
                ?.equals("true", ignoreCase = true) == true

        val sources = buildList {
            if (unprocessedAdvertised) {
                add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.MIC)
        }.distinct()

        for (source in sources) {
            for (rate in listOf(48_000, 44_100)) {
                val minBuffer = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBuffer <= 0) continue

                val bufferBytes = maxOf(minBuffer, rate / 5 * 2)
                val candidate = runCatching {
                    AudioRecord(
                        source,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferBytes,
                    )
                }.getOrNull() ?: continue

                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    return Selection(candidate)
                }
                candidate.release()
            }
        }
        return null
    }

    private fun timingQuality(record: AudioRecord): TimingQuality {
        val timestamp = AudioTimestamp()
        return if (
            record.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC) ==
            AudioRecord.SUCCESS
        ) {
            TimingQuality.AUDIO_TIMESTAMP_MONOTONIC
        } else {
            TimingQuality.MONOTONIC_FALLBACK
        }
    }

    private fun describeRoute(device: AudioDeviceInfo?): String {
        if (device == null) return "No routed device reported"
        return "type=" + device.type + " · " + device.productName
    }

    private fun processingAvailability(): String =
        "availability only: AEC=" + AcousticEchoCanceler.isAvailable() +
            ", AGC=" + AutomaticGainControl.isAvailable() +
            ", NS=" + NoiseSuppressor.isAvailable()

    private fun sourceName(source: Int): String = when (source) {
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        else -> "source-" + source
    }

    private data class Selection(val record: AudioRecord)
}
