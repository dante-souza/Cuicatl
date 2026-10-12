// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin

class StreamingFrequencyWeightingTest {
    private fun gainDb(
        weighting: FrequencyWeighting,
        sampleRateHz: Int,
        toneHz: Double,
    ): Double {
        val processor = StreamingFrequencyWeighting(weighting, sampleRateHz)
        val samples = sampleRateHz * 2
        // Exclude startup transient: use the final one second of steady-state signal.
        var sumInput = 0.0
        var sumOutput = 0.0
        for (index in 0 until samples) {
            val sample = 0.3 * sin(2.0 * PI * toneHz * index / sampleRateHz)
            val output = processor.process(sample)
            if (index >= sampleRateHz) {
                sumInput += sample * sample
                sumOutput += output * output
            }
        }
        return 10.0 * log10(sumOutput / sumInput)
    }

    @Test
    fun zWeightingIsExactIdentityAcrossRates() {
        for (rate in listOf(44_100, 48_000)) {
            val processor = StreamingFrequencyWeighting(FrequencyWeighting.Z, rate)
            for (sample in listOf(-1.0, -0.5, 0.0, 0.125, 0.9)) {
                assertEquals(sample, processor.process(sample), 0.0)
            }
        }
    }

    @Test
    fun aWeightingMatchesResponseAtRepresentativeFrequencies() {
        // Targets are rounded nominal A-weighting responses. Tolerance includes
        // bilinear high-frequency warping; this is NOT a certification test.
        val targets = listOf(
            31.5 to -39.4,
            63.0 to -26.2,
            125.0 to -16.1,
            250.0 to -8.6,
            500.0 to -3.2,
            1000.0 to 0.0,
            2000.0 to 1.2,
            4000.0 to 1.0,
        )
        for (rate in listOf(44_100, 48_000)) {
            for ((hz, targetDb) in targets) {
                assertEquals(
                    "A weighting rate=$rate tone=$hz Hz",
                    targetDb,
                    gainDb(FrequencyWeighting.A, rate, hz),
                    0.35,
                )
            }
        }
    }

    @Test
    fun aWeightingIsIndependentOfBufferBoundaries() {
        for (rate in listOf(44_100, 48_000)) {
            val full = StreamingFrequencyWeighting(FrequencyWeighting.A, rate)
            val segmented = StreamingFrequencyWeighting(FrequencyWeighting.A, rate)
            val samples = DoubleArray(rate) { index ->
                0.3 * sin(2.0 * PI * 137.0 * index / rate)
            }
            val uninterrupted = samples.map(full::process)
            val chunks = samples.toList().chunked(97).flatMap { chunk ->
                chunk.map(segmented::process)
            }
            assertEquals(uninterrupted.size, chunks.size)
            uninterrupted.indices.forEach { index ->
                assertEquals(uninterrupted[index], chunks[index], 1e-12)
            }
        }
    }

    @Test
    fun resetRestoresInitialFilterState() {
        val weighting = StreamingFrequencyWeighting(FrequencyWeighting.A, 48_000)
        val first = weighting.process(0.5)
        repeat(1000) { weighting.process(0.0) }
        weighting.reset()
        assertEquals(first, weighting.process(0.5), 1e-12)
    }

    @Test
    fun rejectsUnverifiedSamplingRates() {
        val result = runCatching {
            StreamingFrequencyWeighting(FrequencyWeighting.A, 32_000)
        }
        assertTrue(result.isFailure)
    }
}
