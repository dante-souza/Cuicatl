// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class MeasurementFrame(
    val sessionId: String,
    val sequence: Long,
    val elapsedStartMillis: Long,
    val durationMillis: Long,
    val sampleRateHz: Int,
    val sampleCount: Int,
    val meanSquareFs: Double,
    val rmsFs: Double,
    val samplePeakFs: Double,
    val rmsDbfs: Double?,
    val samplePeakDbfs: Double?,
    val signalState: SignalState,
)

enum class SignalState {
    NONZERO,
    DIGITAL_ZERO,
    MISSING,
    INVALID,
}
