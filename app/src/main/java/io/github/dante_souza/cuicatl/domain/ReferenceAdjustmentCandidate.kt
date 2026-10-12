// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class ReferenceAdjustmentCandidate(
    val adjustment: ReferenceAdjustment?,
    val rejectionReason: String? = null,
) {
    val isUsable: Boolean get() = adjustment != null && rejectionReason == null
}

object ReferenceAdjustmentFactory {
    fun fromSavedSession(
        detail: SavedSessionDetail,
        deviceModel: String,
        referenceLevelDbSpl: Double,
        referenceMethod: String,
        createdAtUtcEpochMillis: Long,
        notes: String = "",
    ): ReferenceAdjustmentCandidate {
        val session = detail.session
        if (!referenceLevelDbSpl.isFinite()) {
            return ReferenceAdjustmentCandidate(null, "Reference SPL must be finite.")
        }
        if (referenceMethod.isBlank()) {
            return ReferenceAdjustmentCandidate(null, "Reference method is required.")
        }
        if (session.source.isBlank() || session.inputIdentity.isBlank() || session.sampleRateHz <= 0) {
            return ReferenceAdjustmentCandidate(null, "Saved session lacks a complete input configuration.")
        }
        if (detail.frames.any { it.clippedSampleCount > 0 }) {
            return ReferenceAdjustmentCandidate(null, "Reference session contains clipping.")
        }

        val statistics = MeterProjection.statistics(detail.frames, session.frequencyWeighting)
        val measuredLeqDbfs = statistics.leqDbfs
            ?: return ReferenceAdjustmentCandidate(null, "Reference session has no finite Leq.")
        if (statistics.capturedMillis < MIN_REFERENCE_CAPTURE_MS) {
            return ReferenceAdjustmentCandidate(
                null,
                "Reference session must contain at least 10 seconds of valid captured data.",
            )
        }

        return ReferenceAdjustmentCandidate(
            adjustment = ReferenceAdjustment(
                id = "ref-" + session.id,
                version = 1,
                deviceModel = deviceModel,
                inputIdentity = session.inputIdentity,
                audioSource = session.source,
                sampleRateHz = session.sampleRateHz,
                sampleFormat = "PCM16_MONO",
                frequencyWeighting = session.frequencyWeighting,
                referenceMethod = referenceMethod.trim(),
                referenceLevelDbSpl = referenceLevelDbSpl,
                measuredReferenceLevelDbfs = measuredLeqDbfs,
                createdAtUtcEpochMillis = createdAtUtcEpochMillis,
                notes = notes.trim(),
            ),
        )
    }

    private const val MIN_REFERENCE_CAPTURE_MS = 10_000L
}
