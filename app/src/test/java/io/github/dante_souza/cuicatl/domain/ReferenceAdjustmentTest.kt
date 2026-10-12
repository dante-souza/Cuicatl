// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceAdjustmentTest {
    private val configuration = MeasurementInputConfiguration(
        deviceModel = "SM-J810M",
        inputIdentity = "BUILTIN_MIC#1",
        audioSource = "VOICE_RECOGNITION",
        sampleRateHz = 48000,
        sampleFormat = "PCM16_MONO",
        frequencyWeighting = FrequencyWeighting.Z,
    )

    private val adjustment = ReferenceAdjustment(
        id = "j8-z-reference",
        version = 1,
        deviceModel = configuration.deviceModel,
        inputIdentity = configuration.inputIdentity,
        audioSource = configuration.audioSource,
        sampleRateHz = configuration.sampleRateHz,
        sampleFormat = configuration.sampleFormat,
        frequencyWeighting = configuration.frequencyWeighting,
        referenceMethod = "documented_reference_fixture",
        referenceLevelDbSpl = 94.0,
        measuredReferenceLevelDbfs = -26.0,
        createdAtUtcEpochMillis = 1L,
    )

    @Test
    fun correctionIsReferenceMinusMeasuredDigitalLevel() {
        assertEquals(120.0, adjustment.correctionDb, 1e-9)
        assertEquals(
            74.0,
            adjustment.adjustedLevelDbSplEstimate(-46.0, configuration)!!,
            1e-9,
        )
    }

    @Test
    fun inputMismatchDisablesAdjustedEstimate() {
        val changedInput = configuration.copy(inputIdentity = "WIRED_MIC#2")

        assertTrue(
            ReferenceMismatch.INPUT in adjustment.mismatches(changedInput),
        )
        assertNull(
            adjustment.adjustedLevelDbSplEstimate(-46.0, changedInput),
        )
    }

    @Test
    fun weightingMismatchDisablesAdjustedEstimate() {
        val aWeighted = configuration.copy(
            frequencyWeighting = FrequencyWeighting.A,
        )

        assertTrue(
            ReferenceMismatch.FREQUENCY_WEIGHTING in adjustment.mismatches(aWeighted),
        )
        assertNull(
            adjustment.adjustedLevelDbSplEstimate(-46.0, aWeighted),
        )
    }
}
