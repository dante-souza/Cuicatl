// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import kotlin.math.abs

enum class RootlessReferenceMethod {
    ACOUSTIC_CALIBRATOR,
    REFERENCE_SOUND_LEVEL_METER,
    DOCUMENTED_COMPARISON_SOURCE,
}

data class RootlessReferenceProcedure(
    val method: RootlessReferenceMethod,
    val equipmentDescription: String,
    val equipmentIdentifier: String = "",
    val referenceLevelDbSpl: Double,
    val referenceFrequencyHz: Double? = null,
    val referenceUncertaintyDb: Double? = null,
    val geometry: String,
    val environmentNotes: String = "",
    val procedureNotes: String = "",
) {
    init {
        require(equipmentDescription.isNotBlank()) {
            "Reference equipment/source description must not be blank"
        }
        require(referenceLevelDbSpl.isFinite()) {
            "Reference SPL must be finite"
        }
        require(referenceFrequencyHz == null || (referenceFrequencyHz.isFinite() && referenceFrequencyHz > 0.0)) {
            "Reference frequency must be positive when present"
        }
        require(referenceUncertaintyDb == null || (referenceUncertaintyDb.isFinite() && referenceUncertaintyDb >= 0.0)) {
            "Reference uncertainty must be non-negative when present"
        }
        require(geometry.isNotBlank()) {
            "Reference geometry/coupling must not be blank"
        }
    }
}

data class RootlessCalibrationVerification(
    val beforeMeasuredLevelDbfs: Double,
    val afterMeasuredLevelDbfs: Double,
    val maximumAllowedDriftDb: Double,
    val observationCount: Int,
    val notes: String = "",
) {
    init {
        require(beforeMeasuredLevelDbfs.isFinite()) { "Before verification level must be finite" }
        require(afterMeasuredLevelDbfs.isFinite()) { "After verification level must be finite" }
        require(maximumAllowedDriftDb.isFinite() && maximumAllowedDriftDb >= 0.0) {
            "Maximum allowed drift must be finite and non-negative"
        }
        require(observationCount >= MIN_OBSERVATIONS) {
            "At least two reference observations are required"
        }
    }

    val observedDriftDb: Double
        get() = abs(afterMeasuredLevelDbfs - beforeMeasuredLevelDbfs)

    val passes: Boolean
        get() = observedDriftDb <= maximumAllowedDriftDb

    private companion object {
        const val MIN_OBSERVATIONS = 2
    }
}

enum class RootlessCalibrationStatus {
    DRAFT,
    VALIDATED,
    INVALIDATED,
}

enum class RootlessCalibrationEvidenceState {
    UNCALIBRATED,
    REFERENCE_ADJUSTED_ESTIMATE,
    PROFILE_MISMATCH,
}

data class RootlessCalibrationDecision(
    val state: RootlessCalibrationEvidenceState,
    val mismatches: Set<ReferenceMismatch> = emptySet(),
    val reason: String? = null,
) {
    val allowsEstimatedSpl: Boolean
        get() = state == RootlessCalibrationEvidenceState.REFERENCE_ADJUSTED_ESTIMATE
}

data class RootlessCalibrationProfile(
    val id: String,
    val version: Int,
    val sourceSessionId: String,
    val adjustment: ReferenceAdjustment,
    val procedure: RootlessReferenceProcedure,
    val verification: RootlessCalibrationVerification?,
    val status: RootlessCalibrationStatus,
    val createdAtUtcEpochMillis: Long,
    val validatedAtUtcEpochMillis: Long? = null,
) {
    init {
        require(id.isNotBlank()) { "Calibration profile id must not be blank" }
        require(version > 0) { "Calibration profile version must be positive" }
        require(sourceSessionId.isNotBlank()) { "Calibration source session id must not be blank" }
        require(procedure.referenceLevelDbSpl == adjustment.referenceLevelDbSpl) {
            "Procedure reference level must match the adjustment reference level"
        }
        require(
            status != RootlessCalibrationStatus.VALIDATED ||
                (verification != null && verification.passes && validatedAtUtcEpochMillis != null)
        ) {
            "Validated calibration requires a passing verification and validation timestamp"
        }
    }

    fun evaluate(configuration: MeasurementInputConfiguration): RootlessCalibrationDecision {
        if (status != RootlessCalibrationStatus.VALIDATED) {
            return RootlessCalibrationDecision(
                state = RootlessCalibrationEvidenceState.UNCALIBRATED,
                reason =
                    when (status) {
                        RootlessCalibrationStatus.DRAFT ->
                            "Physical reference procedure has not been validated."
                        RootlessCalibrationStatus.INVALIDATED ->
                            "Calibration profile has been invalidated."
                        RootlessCalibrationStatus.VALIDATED ->
                            null
                    },
            )
        }

        val currentVerification = verification
        if (currentVerification == null || !currentVerification.passes) {
            return RootlessCalibrationDecision(
                state = RootlessCalibrationEvidenceState.UNCALIBRATED,
                reason = "Reference repeatability verification did not pass.",
            )
        }

        val mismatches = adjustment.mismatches(configuration)
        if (mismatches.isNotEmpty()) {
            return RootlessCalibrationDecision(
                state = RootlessCalibrationEvidenceState.PROFILE_MISMATCH,
                mismatches = mismatches,
                reason = "Current capture configuration differs from the validated reference profile.",
            )
        }

        return RootlessCalibrationDecision(
            state = RootlessCalibrationEvidenceState.REFERENCE_ADJUSTED_ESTIMATE,
        )
    }

    fun adjustedLevelDbSplEstimate(
        digitalLevelDbfs: Double?,
        configuration: MeasurementInputConfiguration,
    ): Double? {
        if (!evaluate(configuration).allowsEstimatedSpl) return null
        return adjustment.adjustedLevelDbSplEstimate(digitalLevelDbfs, configuration)
    }
}
