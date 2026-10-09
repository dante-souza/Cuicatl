// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.persistence

import android.content.Context
import io.github.dante_souza.cuicatl.domain.FrequencyWeighting
import io.github.dante_souza.cuicatl.domain.ReferenceAdjustment
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

class ReferenceAdjustmentRepository(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)

    fun loadActive(): ReferenceAdjustment? {
        if (!file.isFile) return null
        return runCatching {
            val p = Properties().apply { file.inputStream().use(::load) }
            ReferenceAdjustment(
                id = p.getProperty("id"),
                version = p.getProperty("version").toInt(),
                deviceModel = p.getProperty("device_model"),
                inputIdentity = p.getProperty("input_identity"),
                audioSource = p.getProperty("audio_source"),
                sampleRateHz = p.getProperty("sample_rate_hz").toInt(),
                sampleFormat = p.getProperty("sample_format"),
                frequencyWeighting = FrequencyWeighting.valueOf(p.getProperty("frequency_weighting")),
                referenceMethod = p.getProperty("reference_method"),
                referenceLevelDbSpl = p.getProperty("reference_level_db_spl").toDouble(),
                measuredReferenceLevelDbfs = p.getProperty("measured_reference_level_dbfs").toDouble(),
                createdAtUtcEpochMillis = p.getProperty("created_at_utc_ms").toLong(),
                notes = p.getProperty("notes").orEmpty(),
            )
        }.getOrNull()
    }

    fun saveActive(adjustment: ReferenceAdjustment) {
        val p = Properties().apply {
            setProperty("id", adjustment.id)
            setProperty("version", adjustment.version.toString())
            setProperty("device_model", adjustment.deviceModel)
            setProperty("input_identity", adjustment.inputIdentity)
            setProperty("audio_source", adjustment.audioSource)
            setProperty("sample_rate_hz", adjustment.sampleRateHz.toString())
            setProperty("sample_format", adjustment.sampleFormat)
            setProperty("frequency_weighting", adjustment.frequencyWeighting.name)
            setProperty("reference_method", adjustment.referenceMethod)
            setProperty("reference_level_db_spl", adjustment.referenceLevelDbSpl.toString())
            setProperty("measured_reference_level_dbfs", adjustment.measuredReferenceLevelDbfs.toString())
            setProperty("created_at_utc_ms", adjustment.createdAtUtcEpochMillis.toString())
            setProperty("notes", adjustment.notes)
        }
        val tmp = File(file.parentFile, FILE_NAME + ".tmp")
        FileOutputStream(tmp).use { out ->
            p.store(out, "Cuicatl active reference adjustment")
            out.fd.sync()
        }
        check(tmp.renameTo(file) || runCatching {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
            true
        }.getOrDefault(false)) { "Could not save active reference adjustment" }
    }

    fun clearActive() {
        if (file.exists()) check(file.delete()) { "Could not clear active reference adjustment" }
    }

    private companion object {
        const val FILE_NAME = "active-reference-adjustment.properties"
    }
}
