// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import kotlin.math.log10

data class LevelStatistics(
    val currentDbfs: Double?,
    val minimumDbfs: Double?,
    val maximumDbfs: Double?,
    val leqDbfs: Double?,
    val capturedMillis: Long,
    val intervalCount: Long,
) {
    fun shiftedBy(offsetDb: Double): LevelStatistics {
        require(offsetDb.isFinite()) { "Level offset must be finite" }
        return copy(
            currentDbfs = currentDbfs?.plus(offsetDb),
            minimumDbfs = minimumDbfs?.plus(offsetDb),
            maximumDbfs = maximumDbfs?.plus(offsetDb),
            leqDbfs = leqDbfs?.plus(offsetDb),
        )
    }
}

class LevelStatisticsAccumulator {
    private var currentDbfs: Double? = null
    private var minimumDbfs: Double? = null
    private var maximumDbfs: Double? = null
    private var weightedEnergy = 0.0
    private var capturedMillis = 0L
    private var intervalCount = 0L

    fun addInterval(
        meanSquareFs: Double,
        durationMillis: Long,
    ) {
        require(meanSquareFs.isFinite() && meanSquareFs >= 0.0) {
            "Mean square must be finite and non-negative"
        }
        require(durationMillis > 0L) { "Interval duration must be positive" }

        val levelDbfs = MeasurementMath.dbfsFromMeanSquare(meanSquareFs)
        currentDbfs = levelDbfs

        if (levelDbfs != null) {
            minimumDbfs =
                minimumDbfs?.let { minOf(it, levelDbfs) } ?: levelDbfs
            maximumDbfs =
                maximumDbfs?.let { maxOf(it, levelDbfs) } ?: levelDbfs
        }

        weightedEnergy += meanSquareFs * durationMillis.toDouble()
        capturedMillis += durationMillis
        intervalCount += 1L
    }

    fun snapshot(): LevelStatistics {
        val meanSquare =
            if (capturedMillis > 0L) weightedEnergy / capturedMillis.toDouble()
            else 0.0
        val leq =
            if (meanSquare > 0.0) 10.0 * log10(meanSquare)
            else null

        return LevelStatistics(
            currentDbfs = currentDbfs,
            minimumDbfs = minimumDbfs,
            maximumDbfs = maximumDbfs,
            leqDbfs = leq,
            capturedMillis = capturedMillis,
            intervalCount = intervalCount,
        )
    }
}
