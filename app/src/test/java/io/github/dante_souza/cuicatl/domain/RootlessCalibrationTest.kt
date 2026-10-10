// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootlessCalibrationTest {
    private val config = MeasurementInputConfiguration(
        deviceModel = "SM-J810M",
        inputIdentity = "type=15 · Built-in Mic",
        audioSource = "VOICE_RECOGNITION",
        sampleRateHz = 48000,
        sampleFormat = "PCM16_MONO",
        frequencyWeighting = FrequencyWeighting.A,
    )

    private val adjustment = ReferenceAdjustment(
        id = "ref-session-1",
        version = 1,
        deviceModel = config.deviceModel,
        inputIdentity = config.inputIdentity,
        audioSource = config.audioSource,
        sampleRateHz = config.sampleRateHz,
        sampleFormat = config.sampleFormat,
        frequencyWeighting = config.frequencyWeighting,
        referenceMethod = "Reference meter comparison",
        referenceLevelDbSpl = 80.0,
        measuredReferenceLevelDbfs = -40.0,
        createdAtUtcEpochMillis = 100L,
    )

    private val procedure = RootlessReferenceProcedure(
        method = RootlessReferenceMethod.REFERENCE_SOUND_LEVEL_METER,
        equipmentDescription = "Documented external reference meter",
        equipmentIdentifier = "REF-001",
        referenceLevelDbSpl = 80.0,
        referenceUncertaintyDb = 0.5,
        geometry = "Side-by-side microphones in the same stable sound field",
    )

    private val verification = RootlessCalibrationVerification(
        beforeMeasuredLevelDbfs = -40.0,
        afterMeasuredLevelDbfs = -39.7,
        maximumAllowedDriftDb = 0.5,
        observationCount = 2,
    )

    @Test fun draftProfileNeverEnablesSpl() {
        val profile = RootlessCalibrationProfile(
            id = "cal-1", version = 1, sourceSessionId = "session-1",
            adjustment = adjustment, procedure = procedure, verification = null,
            status = RootlessCalibrationStatus.DRAFT, createdAtUtcEpochMillis = 100L,
        )

        val decision = profile.evaluate(config)
        assertEquals(RootlessCalibrationEvidenceState.UNCALIBRATED, decision.state)
        assertFalse(decision.allowsEstimatedSpl)
        assertNull(profile.adjustedLevelDbSplEstimate(-30.0, config))
    }

    @Test fun validatedMatchingProfileAllowsReferenceAdjustedEstimate() {
        val profile = RootlessCalibrationProfile(
            id = "cal-1", version = 1, sourceSessionId = "session-1",
            adjustment = adjustment, procedure = procedure, verification = verification,
            status = RootlessCalibrationStatus.VALIDATED,
            createdAtUtcEpochMillis = 100L, validatedAtUtcEpochMillis = 200L,
        )

        val decision = profile.evaluate(config)
        assertEquals(RootlessCalibrationEvidenceState.REFERENCE_ADJUSTED_ESTIMATE, decision.state)
        assertTrue(decision.allowsEstimatedSpl)
        assertEquals(90.0, profile.adjustedLevelDbSplEstimate(-30.0, config)!!, 1e-8)
    }

    @Test fun profileMismatchSuppressesEstimate() {
        val profile = RootlessCalibrationProfile(
            id = "cal-1", version = 1, sourceSessionId = "session-1",
            adjustment = adjustment, procedure = procedure, verification = verification,
            status = RootlessCalibrationStatus.VALIDATED,
            createdAtUtcEpochMillis = 100L, validatedAtUtcEpochMillis = 200L,
        )
        val mismatch = config.copy(frequencyWeighting = FrequencyWeighting.Z)

        val decision = profile.evaluate(mismatch)
        assertEquals(RootlessCalibrationEvidenceState.PROFILE_MISMATCH, decision.state)
        assertTrue(decision.mismatches.contains(ReferenceMismatch.FREQUENCY_WEIGHTING))
        assertNull(profile.adjustedLevelDbSplEstimate(-30.0, mismatch))
    }

    @Test fun verificationRecordsAndChecksDrift() {
        val failed = RootlessCalibrationVerification(
            beforeMeasuredLevelDbfs = -40.0,
            afterMeasuredLevelDbfs = -38.9,
            maximumAllowedDriftDb = 0.5,
            observationCount = 2,
        )
        assertEquals(1.1, failed.observedDriftDb, 1e-8)
        assertFalse(failed.passes)
    }
}
