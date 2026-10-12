// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

data class MeasurementInputConfiguration(
    val deviceModel: String,
    val inputIdentity: String,
    val audioSource: String,
    val sampleRateHz: Int,
    val sampleFormat: String,
    val frequencyWeighting: FrequencyWeighting,
)

enum class ReferenceMismatch {
    DEVICE,
    INPUT,
    AUDIO_SOURCE,
    SAMPLE_RATE,
    SAMPLE_FORMAT,
    FREQUENCY_WEIGHTING,
}

data class ReferenceAdjustment(
    val id: String,
    val version: Int,
    val deviceModel: String,
    val inputIdentity: String,
    val audioSource: String,
    val sampleRateHz: Int,
    val sampleFormat: String,
    val frequencyWeighting: FrequencyWeighting,
    val referenceMethod: String,
    val referenceLevelDbSpl: Double,
    val measuredReferenceLevelDbfs: Double,
    val createdAtUtcEpochMillis: Long,
    val notes: String = "",
) {
    init {
        require(id.isNotBlank()) { "Reference adjustment id must not be blank" }
        require(version > 0) { "Reference adjustment version must be positive" }
        require(sampleRateHz > 0) { "Sample rate must be positive" }
        require(referenceMethod.isNotBlank()) { "Reference method must not be blank" }
        require(referenceLevelDbSpl.isFinite()) { "Reference SPL must be finite" }
        require(measuredReferenceLevelDbfs.isFinite()) {
            "Measured reference digital level must be finite"
        }
    }

    val correctionDb: Double
        get() = referenceLevelDbSpl - measuredReferenceLevelDbfs

    fun mismatches(configuration: MeasurementInputConfiguration): Set<ReferenceMismatch> =
        buildSet {
            if (deviceModel != configuration.deviceModel) add(ReferenceMismatch.DEVICE)
            if (inputIdentity != configuration.inputIdentity) add(ReferenceMismatch.INPUT)
            if (audioSource != configuration.audioSource) add(ReferenceMismatch.AUDIO_SOURCE)
            if (sampleRateHz != configuration.sampleRateHz) add(ReferenceMismatch.SAMPLE_RATE)
            if (sampleFormat != configuration.sampleFormat) add(ReferenceMismatch.SAMPLE_FORMAT)
            if (frequencyWeighting != configuration.frequencyWeighting) {
                add(ReferenceMismatch.FREQUENCY_WEIGHTING)
            }
        }

    fun adjustedLevelDbSplEstimate(
        digitalLevelDbfs: Double?,
        configuration: MeasurementInputConfiguration,
    ): Double? {
        if (digitalLevelDbfs == null || !digitalLevelDbfs.isFinite()) return null
        if (mismatches(configuration).isNotEmpty()) return null
        return digitalLevelDbfs + correctionDb
    }
}
