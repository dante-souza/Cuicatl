// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

import android.content.Context
import android.os.Build
import io.github.dante_souza.cuicatl.domain.SavedSessionDetail
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

class MeasurementCsvExporter(
    private val context: Context,
) {
    fun export(detail: SavedSessionDetail): File {
        val exportDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeLabel = detail.session.label
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]+"), "-")
            .trim('-')
            .take(48)
        val stem = if (safeLabel.isBlank()) detail.session.id else safeLabel + "-" + detail.session.id
        val target = File(exportDirectory, stem + ".csv")

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: "unknown"
        val session = detail.session
        val sessionStartUtc = DateTimeFormatter.ISO_INSTANT.format(
            Instant.ofEpochMilli(session.startedAtUtcEpochMillis),
        )

        target.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine(ProvisionalExportFields.candidateColumns.joinToString(","))

            detail.frames.forEach { frame ->
                val intervalUtc = DateTimeFormatter.ISO_INSTANT.format(
                    Instant.ofEpochMilli(
                        session.startedAtUtcEpochMillis + frame.elapsedStartMillis,
                    ),
                )
                val values = listOf(
                    "phase1-draft",
                    session.id,
                    frame.sequence.toString(),
                    CsvFormat.spreadsheetSafeText(session.label),
                    appVersion,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    session.outcome?.name.orEmpty(),
                    sessionStartUtc,
                    session.timezoneOffset,
                    intervalUtc,
                    frame.elapsedStartMillis.toString(),
                    frame.durationMillis.toString(),
                    frame.timingQuality.name,
                    session.inputIdentity,
                    session.source,
                    frame.sampleRateHz.toString(),
                    "PCM16_MONO",
                    frame.sampleCount.toString(),
                    session.processingState,
                    frame.meanSquareFs.toString(),
                    frame.rmsFs.toString(),
                    frame.samplePeakFs.toString(),
                    frame.rmsDbfs?.toString().orEmpty(),
                    frame.samplePeakDbfs?.toString().orEmpty(),
                    "NONE_DIGITAL",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    frame.clippedSampleCount.toString(),
                    frame.signalState.name,
                    frame.qualityFlags.joinToString("|") { it.name },
                    session.elapsedMillis?.toString().orEmpty(),
                    session.capturedMillis.toString(),
                    session.interruptionReason.orEmpty(),
                )

                check(values.size == ProvisionalExportFields.candidateColumns.size) {
                    "CSV row does not match provisional column count"
                }
                writer.appendLine(values.joinToString(",") { CsvFormat.field(it) })
            }
        }
        return target
    }
}
