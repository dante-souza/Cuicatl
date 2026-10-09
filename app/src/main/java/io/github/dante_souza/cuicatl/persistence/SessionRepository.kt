// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.persistence

import android.content.Context
import io.github.dante_souza.cuicatl.domain.MeasurementFrame
import io.github.dante_souza.cuicatl.domain.MeasurementSession
import io.github.dante_souza.cuicatl.domain.QualityFlag
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import io.github.dante_souza.cuicatl.domain.SessionOutcome
import io.github.dante_souza.cuicatl.domain.SessionState
import io.github.dante_souza.cuicatl.domain.SignalState
import io.github.dante_souza.cuicatl.domain.TimingQuality
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

class SessionRepository(context: Context) {
    private val sessionsRoot = File(context.filesDir, "sessions").apply { mkdirs() }

    fun recoverInterruptedSessions(): Int {
        var recovered = 0
        sessionsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { directory ->
                val metadata = readMetadata(directory) ?: return@forEach
                if (metadata.getProperty(KEY_STATE) != SessionState.RUNNING.name) {
                    return@forEach
                }

                val frames = readFrames(directory)
                val capturedMillis = frames.lastOrNull()?.let {
                    it.elapsedStartMillis + it.durationMillis
                } ?: 0L

                metadata.setProperty(KEY_STATE, SessionState.SAVED.name)
                metadata.setProperty(KEY_OUTCOME, SessionOutcome.RECOVERED.name)
                metadata.setProperty(KEY_CAPTURED_MS, capturedMillis.toString())
                metadata.setProperty(KEY_ELAPSED_MS, capturedMillis.toString())
                metadata.setProperty(KEY_FRAME_COUNT, frames.size.toString())
                metadata.setProperty(KEY_INTERRUPTION_REASON, "process_recovery")
                writeMetadata(directory, metadata)
                recovered += 1
            }
        return recovered
    }

    fun beginSession(session: MeasurementSession): ActiveWriter {
        val directory = sessionDirectory(session.id)
        check(!directory.exists()) { "Session already exists: " + session.id }
        check(directory.mkdirs()) { "Could not create session directory" }

        val properties = Properties().apply {
            setProperty(KEY_ID, session.id)
            setProperty(KEY_LABEL, session.label)
            setProperty(KEY_STATE, SessionState.RUNNING.name)
            setProperty(KEY_STARTED_UTC_MS, session.startedAtUtcEpochMillis.toString())
            setProperty(KEY_TIMEZONE_OFFSET, session.timezoneOffset)
            setProperty(KEY_SOURCE, session.source)
            setProperty(KEY_SAMPLE_RATE_HZ, session.sampleRateHz.toString())
            setProperty(KEY_INPUT_IDENTITY, session.inputIdentity)
            setProperty(KEY_PROCESSING_STATE, session.processingState)
            setProperty(KEY_SAMPLE_FORMAT, SAMPLE_FORMAT)
            setProperty(KEY_FRAME_COUNT, "0")
            setProperty(KEY_CAPTURED_MS, "0")
            setProperty(KEY_ELAPSED_MS, "0")
        }
        writeMetadata(directory, properties)

        val frameFile = File(directory, FRAMES_FILE)
        val output = FileOutputStream(frameFile, true)
        output.write((FRAME_HEADER + "\n").toByteArray(Charsets.UTF_8))
        output.flush()
        output.fd.sync()

        return ActiveWriter(directory, output, session)
    }

    fun listSavedSessions(): List<MeasurementSession> =
        sessionsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { directory ->
                val metadata = readMetadata(directory) ?: return@mapNotNull null
                if (metadata.getProperty(KEY_STATE) != SessionState.SAVED.name) {
                    return@mapNotNull null
                }
                sessionFromMetadata(metadata)
            }
            ?.sortedByDescending { it.startedAtUtcEpochMillis }
            ?: emptyList()

    fun loadSession(sessionId: String): SavedSessionDetail? {
        val directory = sessionDirectory(sessionId)
        val metadata = readMetadata(directory) ?: return null
        val session = sessionFromMetadata(metadata)
        if (session.state != SessionState.SAVED) return null
        return SavedSessionDetail(
            session = session,
            frames = readFrames(directory),
        )
    }

    private fun sessionDirectory(sessionId: String): File =
        File(sessionsRoot, sessionId)

    private fun readMetadata(directory: File): Properties? {
        val file = File(directory, METADATA_FILE)
        if (!file.isFile) return null
        return runCatching {
            Properties().apply {
                file.inputStream().use { load(it) }
            }
        }.getOrNull()
    }

    private fun writeMetadata(directory: File, properties: Properties) {
        val target = File(directory, METADATA_FILE)
        val temporary = File(directory, METADATA_TEMP_FILE)
        FileOutputStream(temporary).use { output ->
            properties.store(output, "Cuicatl Phase 1 session metadata")
            output.fd.sync()
        }
        check(temporary.renameTo(target) || copyReplacing(temporary, target)) {
            "Could not finalize session metadata"
        }
    }

    private fun copyReplacing(source: File, target: File): Boolean =
        runCatching {
            source.inputStream().use { input ->
                FileOutputStream(target, false).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            source.delete()
            true
        }.getOrDefault(false)

    private fun readFrames(directory: File): List<MeasurementFrame> {
        val file = File(directory, FRAMES_FILE)
        if (!file.isFile) return emptyList()

        return file.useLines { lines ->
            lines.drop(1).mapNotNull(::parseFrameLine).toList()
        }
    }

    private fun parseFrameLine(line: String): MeasurementFrame? {
        val fields = line.split('\t')
        if (fields.size != FRAME_FIELD_COUNT) return null

        return runCatching {
            MeasurementFrame(
                sessionId = fields[0],
                sequence = fields[1].toLong(),
                elapsedStartMillis = fields[2].toLong(),
                durationMillis = fields[3].toLong(),
                sampleRateHz = fields[4].toInt(),
                sampleCount = fields[5].toInt(),
                meanSquareFs = fields[6].toDouble(),
                rmsFs = fields[7].toDouble(),
                samplePeakFs = fields[8].toDouble(),
                rmsDbfs = fields[9].takeIf { it.isNotEmpty() }?.toDouble(),
                samplePeakDbfs = fields[10].takeIf { it.isNotEmpty() }?.toDouble(),
                signalState = SignalState.valueOf(fields[11]),
                timingQuality = TimingQuality.valueOf(fields[12]),
                clippedSampleCount = fields[13].toInt(),
                qualityFlags = fields[14]
                    .takeIf { it.isNotBlank() }
                    ?.split('|')
                    ?.map { QualityFlag.valueOf(it) }
                    ?.toSet()
                    ?: emptySet(),
            )
        }.getOrNull()
    }

    private fun sessionFromMetadata(metadata: Properties): MeasurementSession =
        MeasurementSession(
            id = metadata.getProperty(KEY_ID),
            state = SessionState.valueOf(metadata.getProperty(KEY_STATE)),
            label = metadata.getProperty(KEY_LABEL).orEmpty(),
            outcome = metadata.getProperty(KEY_OUTCOME)
                ?.takeIf { it.isNotBlank() }
                ?.let(SessionOutcome::valueOf),
            startedAtUtcEpochMillis = metadata.getProperty(KEY_STARTED_UTC_MS, "0").toLong(),
            timezoneOffset = metadata.getProperty(KEY_TIMEZONE_OFFSET).orEmpty(),
            elapsedMillis = metadata.getProperty(KEY_ELAPSED_MS, "0").toLong(),
            capturedMillis = metadata.getProperty(KEY_CAPTURED_MS, "0").toLong(),
            source = metadata.getProperty(KEY_SOURCE).orEmpty(),
            sampleRateHz = metadata.getProperty(KEY_SAMPLE_RATE_HZ, "0").toInt(),
            inputIdentity = metadata.getProperty(KEY_INPUT_IDENTITY).orEmpty(),
            processingState = metadata.getProperty(KEY_PROCESSING_STATE).orEmpty(),
            frameCount = metadata.getProperty(KEY_FRAME_COUNT, "0").toLong(),
            interruptionReason = metadata.getProperty(KEY_INTERRUPTION_REASON)
                ?.takeIf { it.isNotBlank() },
        )

    inner class ActiveWriter internal constructor(
        private val directory: File,
        private val output: FileOutputStream,
        private val baseSession: MeasurementSession,
    ) : AutoCloseable {
        private var frameCount = 0L
        private var capturedMillis = 0L
        private var closed = false

        @Synchronized
        fun append(frame: MeasurementFrame) {
            check(!closed) { "Session writer is closed" }
            val line = listOf(
                frame.sessionId,
                frame.sequence.toString(),
                frame.elapsedStartMillis.toString(),
                frame.durationMillis.toString(),
                frame.sampleRateHz.toString(),
                frame.sampleCount.toString(),
                frame.meanSquareFs.toString(),
                frame.rmsFs.toString(),
                frame.samplePeakFs.toString(),
                frame.rmsDbfs?.toString().orEmpty(),
                frame.samplePeakDbfs?.toString().orEmpty(),
                frame.signalState.name,
                frame.timingQuality.name,
                frame.clippedSampleCount.toString(),
                frame.qualityFlags.joinToString("|") { it.name },
            ).joinToString("\t")

            output.write((line + "\n").toByteArray(Charsets.UTF_8))
            output.flush()

            frameCount += 1
            capturedMillis = frame.elapsedStartMillis + frame.durationMillis
            if (frameCount % SYNC_EVERY_FRAMES == 0L) {
                output.fd.sync()
            }
        }

        @Synchronized
        fun finish(
            outcome: SessionOutcome,
            elapsedMillis: Long,
            interruptionReason: String? = null,
        ): MeasurementSession {
            if (!closed) {
                output.fd.sync()
                output.close()
                closed = true
            }

            val completed = baseSession.copy(
                state = SessionState.SAVED,
                outcome = outcome,
                elapsedMillis = elapsedMillis,
                capturedMillis = capturedMillis,
                frameCount = frameCount,
                interruptionReason = interruptionReason,
            )

            val properties = readMetadata(directory) ?: Properties()
            properties.setProperty(KEY_STATE, SessionState.SAVED.name)
            properties.setProperty(KEY_OUTCOME, outcome.name)
            properties.setProperty(KEY_ELAPSED_MS, elapsedMillis.toString())
            properties.setProperty(KEY_CAPTURED_MS, capturedMillis.toString())
            properties.setProperty(KEY_FRAME_COUNT, frameCount.toString())
            properties.setProperty(KEY_INTERRUPTION_REASON, interruptionReason.orEmpty())
            writeMetadata(directory, properties)
            return completed
        }

        @Synchronized
        override fun close() {
            if (!closed) {
                runCatching { output.fd.sync() }
                runCatching { output.close() }
                closed = true
            }
        }
    }

    private companion object {
        const val METADATA_FILE = "session.properties"
        const val METADATA_TEMP_FILE = "session.properties.tmp"
        const val FRAMES_FILE = "frames.tsv"
        const val SAMPLE_FORMAT = "PCM16_MONO"
        const val SYNC_EVERY_FRAMES = 10L
        const val FRAME_FIELD_COUNT = 15
        const val FRAME_HEADER =
            "session_id\tsequence\telapsed_start_ms\tduration_ms\tsample_rate_hz\tsample_count\t" +
                "mean_square_fs\trms_fs\tsample_peak_fs\trms_dbfs\tsample_peak_dbfs\t" +
                "signal_state\ttiming_quality\tclipped_sample_count\tquality_flags"

        const val KEY_ID = "id"
        const val KEY_LABEL = "label"
        const val KEY_STATE = "state"
        const val KEY_OUTCOME = "outcome"
        const val KEY_STARTED_UTC_MS = "started_utc_ms"
        const val KEY_TIMEZONE_OFFSET = "timezone_offset"
        const val KEY_SOURCE = "source"
        const val KEY_SAMPLE_RATE_HZ = "sample_rate_hz"
        const val KEY_INPUT_IDENTITY = "input_identity"
        const val KEY_PROCESSING_STATE = "processing_state"
        const val KEY_SAMPLE_FORMAT = "sample_format"
        const val KEY_FRAME_COUNT = "frame_count"
        const val KEY_CAPTURED_MS = "captured_ms"
        const val KEY_ELAPSED_MS = "elapsed_ms"
        const val KEY_INTERRUPTION_REASON = "interruption_reason"
    }
}
