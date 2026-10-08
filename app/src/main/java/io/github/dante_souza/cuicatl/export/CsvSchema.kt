// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

object CsvSchema {
    const val VERSION = 1

    val measurementColumns = listOf(
        "schema_version",
        "session_id",
        "segment_id",
        "sequence",
        "interval_start_utc",
        "elapsed_start_ms",
        "duration_ms",
        "sample_rate_hz",
        "sample_count",
        "mean_square_fs",
        "rms_fs",
        "sample_peak_fs",
        "rms_dbfs",
        "sample_peak_dbfs",
        "frequency_weighting",
        "calibration_profile_id",
        "calibration_profile_version",
        "calibration_offset_db",
        "interval_level_db_spl_est",
        "cumulative_leq_db_spl_est",
        "clipped_sample_count",
        "signal_state",
        "quality_flags",
    )
}
