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
    val timingQuality: TimingQuality = TimingQuality.MONOTONIC_FALLBACK,
    val clippedSampleCount: Int = 0,
    val qualityFlags: Set<QualityFlag> = emptySet(),
    val weightedMeanSquareFs: Double? = null,
    val weightedRmsDbfs: Double? = null,
)

enum class SignalState {
    NONZERO,
    DIGITAL_ZERO,
    MISSING,
    INVALID,
}

enum class TimingQuality {
    AUDIO_TIMESTAMP_MONOTONIC,
    MONOTONIC_FALLBACK,
}

enum class QualityFlag {
    CLIPPED,
}
