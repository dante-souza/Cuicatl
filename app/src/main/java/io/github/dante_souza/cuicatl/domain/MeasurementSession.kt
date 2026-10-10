// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class MeasurementSession(
    val id: String,
    val state: SessionState,
    val label: String = "",
    val outcome: SessionOutcome? = null,
    val startedAtUtcEpochMillis: Long = 0L,
    val timezoneOffset: String = "",
    val elapsedMillis: Long? = null,
    val capturedMillis: Long = 0L,
    val source: String = "",
    val sampleRateHz: Int = 0,
    val inputIdentity: String = "",
    val agcRequest: AgcRequest? = null,
    val processingState: String = "",
    val frameCount: Long = 0L,
    val interruptionReason: String? = null,
    val frequencyWeighting: FrequencyWeighting = FrequencyWeighting.Z,
    val calibrationSnapshot: CalibrationSessionSnapshot? = null,
)

enum class SessionState {
    READY,
    STARTING,
    RUNNING,
    FINALIZING,
    SAVED,
}

enum class SessionOutcome {
    COMPLETED,
    INTERRUPTED,
    RECOVERED,
}
