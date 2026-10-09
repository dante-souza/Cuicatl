// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

/** One projection for live, saved and chart interpretations. */
object MeterProjection {
    fun meanSquare(frame: MeasurementFrame, weighting: FrequencyWeighting): Double? {
        if (frame.signalState == SignalState.MISSING || frame.signalState == SignalState.INVALID) return null
        if (frame.durationMillis <= 0L || frame.sampleCount <= 0) return null
        // Never silently substitute unweighted data for absent A-weighted data.
        val energy = when (weighting) {
            FrequencyWeighting.A -> frame.weightedMeanSquareFs
            FrequencyWeighting.Z -> frame.weightedMeanSquareFs ?: frame.meanSquareFs
        }
        return energy?.takeIf { it.isFinite() && it >= 0.0 }
    }

    fun level(frame: MeasurementFrame, weighting: FrequencyWeighting): Double? =
        meanSquare(frame, weighting)?.let(MeasurementMath::dbfsFromMeanSquare)

    fun statistics(frames: List<MeasurementFrame>, weighting: FrequencyWeighting): LevelStatistics {
        val accumulator = LevelStatisticsAccumulator()
        frames.forEach { frame ->
            val energy = meanSquare(frame, weighting)
            if (energy != null) {
                accumulator.addInterval(energy, frame.durationMillis)
            }
        }
        return accumulator.snapshot()
    }
}
