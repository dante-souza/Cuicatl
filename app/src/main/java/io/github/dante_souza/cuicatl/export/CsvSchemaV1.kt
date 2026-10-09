// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

/**
 * Cuicatl measurement CSV schema version 1.
 *
 * Schema 1 was frozen after independent inspection of normal-stop and
 * process-recovery exports on the Samsung Galaxy J8 during Phase 1.
 * Breaking field changes require a new schema version.
 */
object CsvSchemaV1 {
    const val VERSION = "1"

    val columns = listOf(
        "schema_version",
        "session_id",
        "sequence",
        "session_label",
        "app_version",
        "device_model",
        "android_version",
        "session_outcome",
        "session_start_utc",
        "session_timezone_offset",
        "interval_start_utc_estimate",
        "elapsed_start_ms",
        "duration_ms",
        "timing_quality",
        "input_identity",
        "audio_source",
        "sample_rate_hz",
        "sample_format",
        "sample_count",
        "processing_state",
        "mean_square_fs",
        "rms_fs",
        "sample_peak_fs",
        "rms_dbfs",
        "sample_peak_dbfs",
        "frequency_weighting",
        "reference_adjustment_id",
        "reference_adjustment_version",
        "reference_method",
        "reference_level_db",
        "reference_correction_db",
        "interval_level_db_spl_est",
        "cumulative_leq_db_spl_est",
        "clipped_sample_count",
        "signal_state",
        "quality_flags",
        "session_elapsed_ms",
        "session_captured_ms",
        "interruption_reason",
    )
}
