// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.pow

class LevelStatisticsTest {
    @Test
    fun leqUsesEnergyAndActualIntervalDuration() {
        val accumulator = LevelStatisticsAccumulator()
        accumulator.addInterval(
            meanSquareFs = 10.0.pow(-60.0 / 10.0),
            durationMillis = 1000L,
        )
        accumulator.addInterval(
            meanSquareFs = 10.0.pow(-40.0 / 10.0),
            durationMillis = 1000L,
        )

        val statistics = accumulator.snapshot()

        assertEquals(-40.0, statistics.currentDbfs!!, 1e-9)
        assertEquals(-60.0, statistics.minimumDbfs!!, 1e-9)
        assertEquals(-40.0, statistics.maximumDbfs!!, 1e-9)
        assertEquals(-42.967086, statistics.leqDbfs!!, 1e-6)
        assertEquals(2000L, statistics.capturedMillis)
        assertEquals(2L, statistics.intervalCount)
    }

    @Test
    fun digitalZeroContributesDurationWithoutInventingFiniteLevel() {
        val accumulator = LevelStatisticsAccumulator()
        accumulator.addInterval(
            meanSquareFs = 10.0.pow(-40.0 / 10.0),
            durationMillis = 1000L,
        )
        accumulator.addInterval(
            meanSquareFs = 0.0,
            durationMillis = 1000L,
        )

        val statistics = accumulator.snapshot()

        assertNull(statistics.currentDbfs)
        assertEquals(-40.0, statistics.minimumDbfs!!, 1e-9)
        assertEquals(-40.0, statistics.maximumDbfs!!, 1e-9)
        assertEquals(-43.0103, statistics.leqDbfs!!, 1e-4)
        assertEquals(2000L, statistics.capturedMillis)
    }

    @Test
    fun constantReferenceCorrectionShiftsAllFiniteStatistics() {
        val original = LevelStatistics(
            currentDbfs = -30.0,
            minimumDbfs = -50.0,
            maximumDbfs = -20.0,
            leqDbfs = -27.5,
            capturedMillis = 5000L,
            intervalCount = 50L,
        )

        val shifted = original.shiftedBy(120.0)

        assertEquals(90.0, shifted.currentDbfs!!, 1e-9)
        assertEquals(70.0, shifted.minimumDbfs!!, 1e-9)
        assertEquals(100.0, shifted.maximumDbfs!!, 1e-9)
        assertEquals(92.5, shifted.leqDbfs!!, 1e-9)
    }
}
