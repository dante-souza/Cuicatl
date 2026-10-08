// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import kotlin.math.log10
import kotlin.math.sqrt

object MeasurementMath {
    fun meanSquare(samples: DoubleArray): Double {
        require(samples.isNotEmpty()) { "At least one sample is required" }
        return samples.sumOf { it * it } / samples.size
    }

    fun rmsFromMeanSquare(meanSquare: Double): Double {
        require(meanSquare >= 0.0) { "Mean square must be non-negative" }
        return sqrt(meanSquare)
    }

    fun dbfsFromMeanSquare(meanSquare: Double): Double? {
        require(meanSquare >= 0.0) { "Mean square must be non-negative" }
        return if (meanSquare == 0.0) null else 10.0 * log10(meanSquare)
    }

    fun energyEquivalentLevelDb(levelsDb: List<Pair<Double, Double>>): Double? {
        val valid = levelsDb.filter { (_, seconds) -> seconds > 0.0 }
        if (valid.isEmpty()) return null

        val totalDuration = valid.sumOf { it.second }
        val weightedEnergy = valid.sumOf { (levelDb, seconds) ->
            seconds * Math.pow(10.0, levelDb / 10.0)
        }
        return 10.0 * log10(weightedEnergy / totalDuration)
    }
}
