// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeterProjectionTest {
    private fun frame(raw: Double, weighted: Double?, state: SignalState = SignalState.NONZERO) =
        MeasurementFrame(
            sessionId = "test", sequence = 0, elapsedStartMillis = 0,
            durationMillis = 100, sampleRateHz = 48000, sampleCount = 4800,
            meanSquareFs = raw, rmsFs = kotlin.math.sqrt(raw), samplePeakFs = 0.5,
            rmsDbfs = MeasurementMath.dbfsFromMeanSquare(raw),
            samplePeakDbfs = -6.0, signalState = state,
            weightedMeanSquareFs = weighted,
        )

    @Test fun legacyZUsesRawButLegacyAIsUnavailable() {
        val old = frame(0.01, null)
        assertEquals(-20.0, MeterProjection.level(old, FrequencyWeighting.Z)!!, 1e-8)
        assertNull(MeterProjection.level(old, FrequencyWeighting.A))
    }

    @Test fun weightedHistoryUsesFilteredEnergyNotRaw() {
        val a = frame(0.01, 0.0001)
        assertEquals(-40.0, MeterProjection.level(a, FrequencyWeighting.A)!!, 1e-8)
        assertEquals(-20.0, MeterProjection.level(a, FrequencyWeighting.Z)!!, 1e-8)
    }

    @Test fun savedStatisticsSkipMissingAndCountZeroEnergyDuration() {
        val frames = listOf(frame(0.01, 0.01), frame(0.0, 0.0, SignalState.DIGITAL_ZERO),
            frame(0.01, null, SignalState.MISSING))
        val stats = MeterProjection.statistics(frames, FrequencyWeighting.A)
        assertEquals(200L, stats.capturedMillis)
        assertEquals(2L, stats.intervalCount)
        assertNull(stats.currentDbfs)
        assertEquals(-23.0103, stats.leqDbfs!!, 1e-4)
    }
}
