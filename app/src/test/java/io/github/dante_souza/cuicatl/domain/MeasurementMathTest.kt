// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.sqrt

class MeasurementMathTest {
    @Test
    fun fullScaleSineHasMinusThreePointZeroOneDbfsRms() {
        val samples = DoubleArray(48000) { index ->
            kotlin.math.sin(2.0 * Math.PI * 1000.0 * index / 48000.0)
        }

        val meanSquare = MeasurementMath.meanSquare(samples)
        val rms = MeasurementMath.rmsFromMeanSquare(meanSquare)
        val dbfs = MeasurementMath.dbfsFromMeanSquare(meanSquare)

        assertEquals(1.0 / sqrt(2.0), rms, 1e-6)
        assertEquals(-3.0103, dbfs!!, 1e-3)
    }

    @Test
    fun digitalZeroHasNoFiniteDbfsValue() {
        assertNull(MeasurementMath.dbfsFromMeanSquare(0.0))
    }

    @Test
    fun leqUsesEnergyNotArithmeticDecibelAverage() {
        val leq = MeasurementMath.energyEquivalentLevelDb(
            listOf(
                60.0 to 1.0,
                80.0 to 1.0,
            ),
        )

        assertEquals(77.0329, leq!!, 1e-3)
    }
}
