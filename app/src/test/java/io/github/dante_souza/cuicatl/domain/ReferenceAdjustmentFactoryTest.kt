// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ReferenceAdjustmentFactoryTest {
    private fun detail(clipped: Boolean = false): SavedSessionDetail {
        val session = MeasurementSession(
            id = "session-1", state = SessionState.SAVED, source = "VOICE_RECOGNITION",
            sampleRateHz = 48000, inputIdentity = "type=15 · Built-in Mic",
            frequencyWeighting = FrequencyWeighting.A,
        )
        val frames = List(100) { index ->
            MeasurementFrame(
                sessionId = session.id, sequence = index.toLong(),
                elapsedStartMillis = index * 100L, durationMillis = 100L,
                sampleRateHz = 48000, sampleCount = 4800,
                meanSquareFs = 10.0.pow(-40.0 / 10.0),
                rmsFs = 0.01, samplePeakFs = 0.02,
                rmsDbfs = -40.0, samplePeakDbfs = -34.0,
                signalState = SignalState.NONZERO,
                clippedSampleCount = if (clipped && index == 1) 1 else 0,
                weightedMeanSquareFs = 10.0.pow(-50.0 / 10.0),
                weightedRmsDbfs = -50.0,
            )
        }
        return SavedSessionDetail(session, frames)
    }

    @Test fun createsAdjustmentFromValidReferenceSessionLeq() {
        val result = ReferenceAdjustmentFactory.fromSavedSession(
            detail(), "SM-J810M", 94.0, "acoustic calibrator", 123L
        )
        assertTrue(result.isUsable)
        val adjustment = result.adjustment!!
        assertEquals(-50.0, adjustment.measuredReferenceLevelDbfs, 1e-8)
        assertEquals(144.0, adjustment.correctionDb, 1e-8)
        assertEquals(FrequencyWeighting.A, adjustment.frequencyWeighting)
    }

    @Test fun rejectsClippedReferenceSession() {
        val result = ReferenceAdjustmentFactory.fromSavedSession(
            detail(clipped = true), "SM-J810M", 94.0, "reference meter", 123L
        )
        assertFalse(result.isUsable)
        assertTrue(result.rejectionReason!!.contains("clipping"))
    }

    @Test fun rejectsShortReferenceSession() {
        val short = detail().copy(frames = detail().frames.take(20))
        val result = ReferenceAdjustmentFactory.fromSavedSession(
            short, "SM-J810M", 94.0, "reference meter", 123L
        )
        assertFalse(result.isUsable)
    }
}
