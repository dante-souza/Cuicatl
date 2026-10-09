// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class SessionRuntimeSnapshot(
    val status: Status = Status.READY,
    val activeSession: MeasurementSession? = null,
    val currentFrame: MeasurementFrame? = null,
    val history: List<MeasurementFrame> = emptyList(),
    val clippedFrameCount: Long = 0L,
    val clippedSampleCount: Long = 0L,
    val message: String = "Ready. Microphone capture starts only after Start.",
) {
    enum class Status {
        READY,
        STARTING,
        RUNNING,
        FINALIZING,
        FAILED,
    }
}

data class SavedSessionDetail(
    val session: MeasurementSession,
    val frames: List<MeasurementFrame>,
)
