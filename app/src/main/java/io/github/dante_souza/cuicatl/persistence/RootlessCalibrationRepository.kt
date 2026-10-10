// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.persistence

import android.content.Context
import io.github.dante_souza.cuicatl.domain.FrequencyWeighting
import io.github.dante_souza.cuicatl.domain.ReferenceAdjustment
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationProfile
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationStatus
import io.github.dante_souza.cuicatl.domain.RootlessCalibrationVerification
import io.github.dante_souza.cuicatl.domain.RootlessReferenceMethod
import io.github.dante_souza.cuicatl.domain.RootlessReferenceProcedure
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

class RootlessCalibrationRepository(context: Context) {
    private val profilesRoot =
        File(context.filesDir, PROFILES_DIRECTORY).apply { mkdirs() }
    private val activeFile = File(context.filesDir, ACTIVE_FILE_NAME)

    fun loadActive(): RootlessCalibrationProfile? {
        if (!activeFile.isFile) return null
        val pointer = readProperties(activeFile) ?: return null
        val id =
            pointer.getProperty(KEY_PROFILE_ID)
                ?.takeIf { it.isNotBlank() }
                ?: return null
        val version =
            pointer.getProperty(KEY_PROFILE_VERSION)?.toIntOrNull()
                ?: return null
        return loadProfile(id, version)
    }

    fun loadProfile(id: String, version: Int): RootlessCalibrationProfile? {
        val file = profileFile(id, version)
        if (!file.isFile) return null
        val properties = readProperties(file) ?: return null
        return decodeProfile(properties)
    }

    fun saveNew(profile: RootlessCalibrationProfile) {
        val target = profileFile(profile.id, profile.version)
        check(!target.exists()) {
            "Calibration profile already exists: ${profile.id} v${profile.version}"
        }
        writeAtomic(
            target = target,
            properties = encodeProfile(profile),
            comment = "Cuicatl rootless calibration profile",
        )
    }

    fun setActive(profile: RootlessCalibrationProfile) {
        check(loadProfile(profile.id, profile.version) != null) {
            "Calibration profile must be persisted before it can become active"
        }
        val properties = Properties().apply {
            setProperty(KEY_PROFILE_ID, profile.id)
            setProperty(KEY_PROFILE_VERSION, profile.version.toString())
        }
        writeAtomic(
            target = activeFile,
            properties = properties,
            comment = "Cuicatl active rootless calibration profile pointer",
        )
    }

    fun clearActive() {
        if (activeFile.exists()) {
            check(activeFile.delete()) {
                "Could not clear active calibration profile"
            }
        }
    }

    fun saveDraft(
        adjustment: ReferenceAdjustment,
        sourceSessionId: String,
        procedure: RootlessReferenceProcedure,
    ): RootlessCalibrationProfile {
        val profileId = "cal-" + sourceSessionId
        val profile = RootlessCalibrationProfile(
            id = profileId,
            version = nextVersion(profileId),
            sourceSessionId = sourceSessionId,
            adjustment = adjustment,
            procedure = procedure,
            verification = null,
            status = RootlessCalibrationStatus.DRAFT,
            createdAtUtcEpochMillis = adjustment.createdAtUtcEpochMillis,
            validatedAtUtcEpochMillis = null,
        )
        saveNew(profile)
        setActive(profile)
        return profile
    }

    fun saveDraftFromAdjustment(
        adjustment: ReferenceAdjustment,
        sourceSessionId: String,
    ): RootlessCalibrationProfile =
        saveDraft(
            adjustment = adjustment,
            sourceSessionId = sourceSessionId,
            procedure = RootlessReferenceProcedure(
                method = RootlessReferenceMethod.DOCUMENTED_COMPARISON_SOURCE,
                equipmentDescription = adjustment.referenceMethod,
                referenceLevelDbSpl = adjustment.referenceLevelDbSpl,
                geometry =
                    adjustment.notes.takeIf { it.isNotBlank() }
                        ?: "Geometry not recorded in Phase 2C.1 draft; physical validation required.",
                procedureNotes =
                    adjustment.notes.takeIf { it.isNotBlank() }
                        ?: "Migrated/drafted from Phase 2C.1 reference-adjustment input.",
            ),
        )

    fun recordActiveVerification(
        verification: RootlessCalibrationVerification,
        observedAtUtcEpochMillis: Long,
    ): RootlessCalibrationProfile {
        val active =
            loadActive()
                ?: error("No active calibration profile")
        val validated = verification.passes
        val revised = active.copy(
            version = nextVersion(active.id),
            verification = verification,
            status =
                if (validated) {
                    RootlessCalibrationStatus.VALIDATED
                } else {
                    RootlessCalibrationStatus.DRAFT
                },
            validatedAtUtcEpochMillis =
                if (validated) observedAtUtcEpochMillis else null,
        )
        saveNew(revised)
        setActive(revised)
        return revised
    }

    fun migrateLegacyActiveAdjustment(
        legacyRepository: ReferenceAdjustmentRepository,
    ): RootlessCalibrationProfile? {
        loadActive()?.let { return it }

        val legacy = legacyRepository.loadActive() ?: return null
        val sourceSessionId =
            legacy.id.removePrefix("ref-").takeIf { it.isNotBlank() }
                ?: "legacy-" + legacy.createdAtUtcEpochMillis

        val migrated = saveDraftFromAdjustment(
            adjustment = legacy,
            sourceSessionId = sourceSessionId,
        )

        // Retire the legacy pointer only after the new profile and active pointer are durable.
        legacyRepository.clearActive()
        return migrated
    }

    fun deleteProfilesFromSourceSession(sessionId: String): Int {
        var deleted = 0
        val active = loadActive()
        var removedActive = false

        profileFiles().forEach { file ->
            val profile =
                readProperties(file)?.let(::decodeProfile)
                    ?: return@forEach
            if (profile.sourceSessionId == sessionId && file.delete()) {
                deleted += 1
                if (
                    active != null &&
                    active.id == profile.id &&
                    active.version == profile.version
                ) {
                    removedActive = true
                }
            }
        }

        if (removedActive) clearActive()
        return deleted
    }

    fun clearAll(): Int {
        clearActive()
        var deleted = 0
        profileFiles().forEach {
            if (it.delete()) deleted += 1
        }
        return deleted
    }

    private fun nextVersion(profileId: String): Int =
        profileFiles()
            .mapNotNull { file ->
                readProperties(file)
                    ?.let(::decodeProfile)
                    ?.takeIf { it.id == profileId }
                    ?.version
            }
            .maxOrNull()
            ?.plus(1)
            ?: 1

    private fun profileFiles(): List<File> =
        profilesRoot.listFiles()
            ?.filter { it.isFile && it.extension == PROFILE_EXTENSION }
            ?: emptyList()

    private fun profileFile(id: String, version: Int): File {
        val safeId = id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(
            profilesRoot,
            "$safeId-v$version.$PROFILE_EXTENSION",
        )
    }

    private fun encodeProfile(
        profile: RootlessCalibrationProfile,
    ): Properties =
        Properties().apply {
            setProperty(KEY_PROFILE_ID, profile.id)
            setProperty(KEY_PROFILE_VERSION, profile.version.toString())
            setProperty(KEY_SOURCE_SESSION_ID, profile.sourceSessionId)
            setProperty(KEY_STATUS, profile.status.name)
            setProperty(
                KEY_CREATED_AT_UTC_MS,
                profile.createdAtUtcEpochMillis.toString(),
            )
            profile.validatedAtUtcEpochMillis?.let {
                setProperty(KEY_VALIDATED_AT_UTC_MS, it.toString())
            }

            val adjustment = profile.adjustment
            setProperty(KEY_ADJUSTMENT_ID, adjustment.id)
            setProperty(
                KEY_ADJUSTMENT_VERSION,
                adjustment.version.toString(),
            )
            setProperty(KEY_DEVICE_MODEL, adjustment.deviceModel)
            setProperty(KEY_INPUT_IDENTITY, adjustment.inputIdentity)
            setProperty(KEY_AUDIO_SOURCE, adjustment.audioSource)
            setProperty(
                KEY_SAMPLE_RATE_HZ,
                adjustment.sampleRateHz.toString(),
            )
            setProperty(KEY_SAMPLE_FORMAT, adjustment.sampleFormat)
            setProperty(
                KEY_FREQUENCY_WEIGHTING,
                adjustment.frequencyWeighting.name,
            )
            setProperty(
                KEY_REFERENCE_METHOD_TEXT,
                adjustment.referenceMethod,
            )
            setProperty(
                KEY_REFERENCE_LEVEL_DB_SPL,
                adjustment.referenceLevelDbSpl.toString(),
            )
            setProperty(
                KEY_MEASURED_REFERENCE_LEVEL_DBFS,
                adjustment.measuredReferenceLevelDbfs.toString(),
            )
            setProperty(
                KEY_ADJUSTMENT_CREATED_AT_UTC_MS,
                adjustment.createdAtUtcEpochMillis.toString(),
            )
            setProperty(KEY_ADJUSTMENT_NOTES, adjustment.notes)

            val procedure = profile.procedure
            setProperty(KEY_PROCEDURE_METHOD, procedure.method.name)
            setProperty(
                KEY_EQUIPMENT_DESCRIPTION,
                procedure.equipmentDescription,
            )
            setProperty(
                KEY_EQUIPMENT_IDENTIFIER,
                procedure.equipmentIdentifier,
            )
            setProperty(
                KEY_PROCEDURE_REFERENCE_LEVEL_DB_SPL,
                procedure.referenceLevelDbSpl.toString(),
            )
            procedure.referenceFrequencyHz?.let {
                setProperty(KEY_REFERENCE_FREQUENCY_HZ, it.toString())
            }
            procedure.referenceUncertaintyDb?.let {
                setProperty(KEY_REFERENCE_UNCERTAINTY_DB, it.toString())
            }
            setProperty(KEY_GEOMETRY, procedure.geometry)
            setProperty(
                KEY_ENVIRONMENT_NOTES,
                procedure.environmentNotes,
            )
            setProperty(KEY_PROCEDURE_NOTES, procedure.procedureNotes)

            profile.verification?.let { verification ->
                setProperty(
                    KEY_VERIFICATION_BEFORE_DBFS,
                    verification.beforeMeasuredLevelDbfs.toString(),
                )
                setProperty(
                    KEY_VERIFICATION_AFTER_DBFS,
                    verification.afterMeasuredLevelDbfs.toString(),
                )
                setProperty(
                    KEY_VERIFICATION_MAX_DRIFT_DB,
                    verification.maximumAllowedDriftDb.toString(),
                )
                setProperty(
                    KEY_VERIFICATION_OBSERVATION_COUNT,
                    verification.observationCount.toString(),
                )
                setProperty(
                    KEY_VERIFICATION_NOTES,
                    verification.notes,
                )
            }
        }

    private fun decodeProfile(
        p: Properties,
    ): RootlessCalibrationProfile? =
        runCatching {
            val adjustment = ReferenceAdjustment(
                id = p.getProperty(KEY_ADJUSTMENT_ID),
                version =
                    p.getProperty(KEY_ADJUSTMENT_VERSION).toInt(),
                deviceModel = p.getProperty(KEY_DEVICE_MODEL),
                inputIdentity = p.getProperty(KEY_INPUT_IDENTITY),
                audioSource = p.getProperty(KEY_AUDIO_SOURCE),
                sampleRateHz =
                    p.getProperty(KEY_SAMPLE_RATE_HZ).toInt(),
                sampleFormat = p.getProperty(KEY_SAMPLE_FORMAT),
                frequencyWeighting =
                    FrequencyWeighting.valueOf(
                        p.getProperty(KEY_FREQUENCY_WEIGHTING),
                    ),
                referenceMethod =
                    p.getProperty(KEY_REFERENCE_METHOD_TEXT),
                referenceLevelDbSpl =
                    p.getProperty(KEY_REFERENCE_LEVEL_DB_SPL).toDouble(),
                measuredReferenceLevelDbfs =
                    p.getProperty(
                        KEY_MEASURED_REFERENCE_LEVEL_DBFS,
                    ).toDouble(),
                createdAtUtcEpochMillis =
                    p.getProperty(
                        KEY_ADJUSTMENT_CREATED_AT_UTC_MS,
                    ).toLong(),
                notes =
                    p.getProperty(KEY_ADJUSTMENT_NOTES).orEmpty(),
            )

            val procedure = RootlessReferenceProcedure(
                method =
                    RootlessReferenceMethod.valueOf(
                        p.getProperty(KEY_PROCEDURE_METHOD),
                    ),
                equipmentDescription =
                    p.getProperty(KEY_EQUIPMENT_DESCRIPTION),
                equipmentIdentifier =
                    p.getProperty(KEY_EQUIPMENT_IDENTIFIER).orEmpty(),
                referenceLevelDbSpl =
                    p.getProperty(
                        KEY_PROCEDURE_REFERENCE_LEVEL_DB_SPL,
                    ).toDouble(),
                referenceFrequencyHz =
                    p.getProperty(KEY_REFERENCE_FREQUENCY_HZ)
                        ?.toDoubleOrNull(),
                referenceUncertaintyDb =
                    p.getProperty(KEY_REFERENCE_UNCERTAINTY_DB)
                        ?.toDoubleOrNull(),
                geometry = p.getProperty(KEY_GEOMETRY),
                environmentNotes =
                    p.getProperty(KEY_ENVIRONMENT_NOTES).orEmpty(),
                procedureNotes =
                    p.getProperty(KEY_PROCEDURE_NOTES).orEmpty(),
            )

            val verification =
                p.getProperty(KEY_VERIFICATION_BEFORE_DBFS)?.let {
                    RootlessCalibrationVerification(
                        beforeMeasuredLevelDbfs = it.toDouble(),
                        afterMeasuredLevelDbfs =
                            p.getProperty(
                                KEY_VERIFICATION_AFTER_DBFS,
                            ).toDouble(),
                        maximumAllowedDriftDb =
                            p.getProperty(
                                KEY_VERIFICATION_MAX_DRIFT_DB,
                            ).toDouble(),
                        observationCount =
                            p.getProperty(
                                KEY_VERIFICATION_OBSERVATION_COUNT,
                            ).toInt(),
                        notes =
                            p.getProperty(
                                KEY_VERIFICATION_NOTES,
                            ).orEmpty(),
                    )
                }

            RootlessCalibrationProfile(
                id = p.getProperty(KEY_PROFILE_ID),
                version = p.getProperty(KEY_PROFILE_VERSION).toInt(),
                sourceSessionId =
                    p.getProperty(KEY_SOURCE_SESSION_ID),
                adjustment = adjustment,
                procedure = procedure,
                verification = verification,
                status =
                    RootlessCalibrationStatus.valueOf(
                        p.getProperty(KEY_STATUS),
                    ),
                createdAtUtcEpochMillis =
                    p.getProperty(KEY_CREATED_AT_UTC_MS).toLong(),
                validatedAtUtcEpochMillis =
                    p.getProperty(KEY_VALIDATED_AT_UTC_MS)
                        ?.toLongOrNull(),
            )
        }.getOrNull()

    private fun readProperties(file: File): Properties? =
        runCatching {
            Properties().apply {
                file.inputStream().use { load(it) }
            }
        }.getOrNull()

    private fun writeAtomic(
        target: File,
        properties: Properties,
        comment: String,
    ) {
        target.parentFile?.mkdirs()
        val temporary = File(
            target.parentFile,
            target.name + ".tmp",
        )
        FileOutputStream(temporary).use { output ->
            properties.store(output, comment)
            output.fd.sync()
        }
        check(
            temporary.renameTo(target) ||
                runCatching {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                    true
                }.getOrDefault(false),
        ) {
            "Could not finalize calibration profile file: ${target.name}"
        }
    }

    private companion object {
        const val PROFILES_DIRECTORY = "rootless-calibration-profiles"
        const val ACTIVE_FILE_NAME =
            "active-rootless-calibration.properties"
        const val PROFILE_EXTENSION = "properties"

        const val KEY_PROFILE_ID = "profile_id"
        const val KEY_PROFILE_VERSION = "profile_version"
        const val KEY_SOURCE_SESSION_ID = "source_session_id"
        const val KEY_STATUS = "status"
        const val KEY_CREATED_AT_UTC_MS = "created_at_utc_ms"
        const val KEY_VALIDATED_AT_UTC_MS = "validated_at_utc_ms"

        const val KEY_ADJUSTMENT_ID = "adjustment_id"
        const val KEY_ADJUSTMENT_VERSION = "adjustment_version"
        const val KEY_DEVICE_MODEL = "device_model"
        const val KEY_INPUT_IDENTITY = "input_identity"
        const val KEY_AUDIO_SOURCE = "audio_source"
        const val KEY_SAMPLE_RATE_HZ = "sample_rate_hz"
        const val KEY_SAMPLE_FORMAT = "sample_format"
        const val KEY_FREQUENCY_WEIGHTING = "frequency_weighting"
        const val KEY_REFERENCE_METHOD_TEXT =
            "reference_method_text"
        const val KEY_REFERENCE_LEVEL_DB_SPL =
            "reference_level_db_spl"
        const val KEY_MEASURED_REFERENCE_LEVEL_DBFS =
            "measured_reference_level_dbfs"
        const val KEY_ADJUSTMENT_CREATED_AT_UTC_MS =
            "adjustment_created_at_utc_ms"
        const val KEY_ADJUSTMENT_NOTES = "adjustment_notes"

        const val KEY_PROCEDURE_METHOD = "procedure_method"
        const val KEY_EQUIPMENT_DESCRIPTION =
            "equipment_description"
        const val KEY_EQUIPMENT_IDENTIFIER =
            "equipment_identifier"
        const val KEY_PROCEDURE_REFERENCE_LEVEL_DB_SPL =
            "procedure_reference_level_db_spl"
        const val KEY_REFERENCE_FREQUENCY_HZ =
            "reference_frequency_hz"
        const val KEY_REFERENCE_UNCERTAINTY_DB =
            "reference_uncertainty_db"
        const val KEY_GEOMETRY = "geometry"
        const val KEY_ENVIRONMENT_NOTES = "environment_notes"
        const val KEY_PROCEDURE_NOTES = "procedure_notes"

        const val KEY_VERIFICATION_BEFORE_DBFS =
            "verification_before_dbfs"
        const val KEY_VERIFICATION_AFTER_DBFS =
            "verification_after_dbfs"
        const val KEY_VERIFICATION_MAX_DRIFT_DB =
            "verification_max_drift_db"
        const val KEY_VERIFICATION_OBSERVATION_COUNT =
            "verification_observation_count"
        const val KEY_VERIFICATION_NOTES = "verification_notes"
    }
}
