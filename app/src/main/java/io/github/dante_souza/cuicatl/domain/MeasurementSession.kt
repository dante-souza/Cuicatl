// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class MeasurementSession(
    val id: String,
    val state: SessionState,
    val outcome: SessionOutcome? = null,
    val elapsedMillis: Long = 0L,
    val capturedMillis: Long = 0L,
)

enum class SessionState {
    READY,
    STARTING,
    RUNNING,
    PAUSED,
    FINALIZING,
    SAVED,
}

enum class SessionOutcome {
    COMPLETED,
    INTERRUPTED,
    RECOVERED,
}
