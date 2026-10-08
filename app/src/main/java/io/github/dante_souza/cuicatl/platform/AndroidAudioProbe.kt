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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.github.dante_souza.cuicatl.domain.MeasurementMath
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.log10

data class AudioProbeSnapshot(
    val status: Status = Status.IDLE,
    val source: String = "—",
    val sampleRateHz: Int? = null,
    val sampleCount: Long = 0,
    val elapsedMillis: Long = 0,
    val rmsDbfs: Double? = null,
    val peakDbfs: Double? = null,
    val route: String = "Not routed",
    val timing: String = "Not sampled",
    val processing: String = "Not probed",
    val attemptedConfigurations: String = "",
    val message: String = "Ready for explicit diagnostic capture.",
) {
    enum class Status {
        IDLE,
        RUNNING,
        COMPLETED,
        FAILED,
    }
}

class AndroidAudioProbe(
    context: Context,
    private val onSnapshot: (AudioProbeSnapshot) -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    @Volatile
    private var activeRecord: AudioRecord? = null

    @Volatile
    private var manuallyStopped = false

    fun start() {
        if (!running.compareAndSet(false, true)) return
        manuallyStopped = false

        thread(name = "cuicatl-phase0-audio-probe", isDaemon = true) {
            runProbe()
        }
    }

    fun stop() {
        manuallyStopped = true
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
    private fun runProbe() {
        val attempted = mutableListOf<String>()
        var record: AudioRecord? = null

        try {
            val selection = openFirstSupportedRecord(attempted)
            if (selection == null) {
                publish(
                    AudioProbeSnapshot(
                        status = AudioProbeSnapshot.Status.FAILED,
                        attemptedConfigurations = attempted.joinToString(),
                        message = "No tested mono PCM16 source/rate combination initialized.",
                    ),
                )
                return
            }

            record = selection.record
            activeRecord = record

            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                error("AudioRecord did not enter RECORDSTATE_RECORDING")
            }

            val sourceName = sourceName(record.audioSource)
            val rate = record.sampleRate
            val buffer = ShortArray(maxOf(1024, rate / 10))
            val startedAt = SystemClock.elapsedRealtime()
            val maxDurationMs = 10_000L
            var sampleCount = 0L
            var lastRmsDbfs: Double? = null
            var lastPeakDbfs: Double? = null
            val processing = processingAvailability()

            publish(
                AudioProbeSnapshot(
                    status = AudioProbeSnapshot.Status.RUNNING,
                    source = sourceName,
                    sampleRateHz = rate,
                    route = describeRoute(record.routedDevice),
                    timing = describeTiming(record),
                    processing = processing,
                    attemptedConfigurations = attempted.joinToString(),
                    message = "Capturing diagnostic PCM for up to 10 seconds.",
                ),
            )

            while (running.get() && SystemClock.elapsedRealtime() - startedAt < maxDurationMs) {
                val read = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read < 0) {
                    if (!running.get()) break
                    error("AudioRecord.read failed with code $read")
                }
                if (read == 0) continue

                var sumSquares = 0.0
                var peak = 0.0
                for (index in 0 until read) {
                    val normalized = buffer[index].toDouble() / 32768.0
                    sumSquares += normalized * normalized
                    peak = maxOf(peak, abs(normalized))
                }

                sampleCount += read
                val meanSquare = sumSquares / read
                val rmsDbfs = MeasurementMath.dbfsFromMeanSquare(meanSquare)
                val peakDbfs = if (peak == 0.0) null else 20.0 * log10(peak)
                lastRmsDbfs = rmsDbfs
                lastPeakDbfs = peakDbfs
                val elapsed = SystemClock.elapsedRealtime() - startedAt

                publish(
                    AudioProbeSnapshot(
                        status = AudioProbeSnapshot.Status.RUNNING,
                        source = sourceName,
                        sampleRateHz = rate,
                        sampleCount = sampleCount,
                        elapsedMillis = elapsed,
                        rmsDbfs = rmsDbfs,
                        peakDbfs = peakDbfs,
                        route = describeRoute(record.routedDevice),
                        timing = describeTiming(record),
                        processing = processing,
                        attemptedConfigurations = attempted.joinToString(),
                        message = "Diagnostic capture active. Values are digital dBFS, not SPL.",
                    ),
                )
            }

            running.set(false)
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }

            val elapsed = SystemClock.elapsedRealtime() - startedAt
            publish(
                AudioProbeSnapshot(
                    status = AudioProbeSnapshot.Status.COMPLETED,
                    source = sourceName,
                    sampleRateHz = rate,
                    sampleCount = sampleCount,
                    elapsedMillis = elapsed,
                    rmsDbfs = lastRmsDbfs,
                    peakDbfs = lastPeakDbfs,
                    route = describeRoute(record.routedDevice),
                    timing = describeTiming(record),
                    processing = processing,
                    attemptedConfigurations = attempted.joinToString(),
                    message = if (manuallyStopped) {
                        "Diagnostic capture stopped by user."
                    } else {
                        "10-second diagnostic capture completed."
                    },
                ),
            )
        } catch (security: SecurityException) {
            publish(
                AudioProbeSnapshot(
                    status = AudioProbeSnapshot.Status.FAILED,
                    attemptedConfigurations = attempted.joinToString(),
                    message = "Microphone permission or platform policy blocked capture: ${security.message}",
                ),
            )
        } catch (error: Throwable) {
            publish(
                AudioProbeSnapshot(
                    status = AudioProbeSnapshot.Status.FAILED,
                    attemptedConfigurations = attempted.joinToString(),
                    message = "Diagnostic capture failed: ${error.message ?: error::class.java.simpleName}",
                ),
            )
        } finally {
            running.set(false)
            activeRecord = null
            runCatching { record?.release() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openFirstSupportedRecord(attempted: MutableList<String>): Selection? {
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
                val label = "${sourceName(source)}@${rate}"
                val minBuffer = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBuffer <= 0) {
                    attempted += "${label}:minBuffer=${minBuffer}"
                    continue
                }

                val bufferBytes = maxOf(minBuffer, rate / 5 * 2)
                val candidate = try {
                    AudioRecord(
                        source,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferBytes,
                    )
                } catch (error: Throwable) {
                    attempted += "${label}:create=${error::class.java.simpleName}"
                    continue
                }

                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    attempted += "${label}:initialized"
                    return Selection(candidate)
                }

                attempted += "${label}:state=${candidate.state}"
                candidate.release()
            }
        }

        return null
    }

    private fun describeTiming(record: AudioRecord): String {
        val timestamp = AudioTimestamp()
        val status = record.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC)
        return if (status == AudioRecord.SUCCESS) {
            "AudioTimestamp monotonic · frame=${timestamp.framePosition} · nano=${timestamp.nanoTime}"
        } else {
            "AudioTimestamp unavailable (${status}); elapsedRealtime fallback observed"
        }
    }

    private fun describeRoute(device: AudioDeviceInfo?): String {
        if (device == null) return "No routed device reported"
        return "type=${device.type} · ${device.productName}"
    }

    private fun processingAvailability(): String =
        "platform availability only: AEC=${AcousticEchoCanceler.isAvailable()}, " +
            "AGC=${AutomaticGainControl.isAvailable()}, NS=${NoiseSuppressor.isAvailable()}"

    private fun sourceName(source: Int): String = when (source) {
        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        else -> "source-${source}"
    }

    private fun publish(snapshot: AudioProbeSnapshot) {
        Log.i(TAG, snapshot.toString())
        mainHandler.post { onSnapshot(snapshot) }
    }

    private data class Selection(val record: AudioRecord)

    private companion object {
        const val TAG = "CuicatlAudioProbe"
    }
}
