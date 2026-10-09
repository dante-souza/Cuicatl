// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

/**
 * Stateful PCM frequency-weighting processor. One instance belongs to one fixed-rate,
 * fixed-weighting measurement session. Never reset between read buffers or intervals.
 *
 * A: three second-order sections obtained by bilinear-transforming the standard
 * six-pole/four-zero analogue A-weighting response. Gain normalized to 0 dB at
 * 1 kHz for each supported digital sampling rate.
 * Z: identity digital path, NOT a microphone flatness claim.
 *
 * Frequencies approaching Nyquist are warped; these coefficients have a
 * declared verification band rather than an IEC sound-level-meter class claim.
 */
class StreamingFrequencyWeighting(
    val weighting: FrequencyWeighting,
    val sampleRateHz: Int,
) {
    init {
        require(sampleRateHz == 48_000 || sampleRateHz == 44_100) {
            "A/Z response has been designed only for 48000 or 44100 Hz"
        }
    }

    private val sections: Array<Biquad> =
        if (weighting == FrequencyWeighting.Z) emptyArray()
        else when (sampleRateHz) {
            48_000 -> arrayOf(
                Biquad(0.23418304260356068, 0.46836608520712136, 0.23418304260356068, -0.22455845805977914, 0.0126066252715464),
                Biquad(1.0, -2.0, 1.0, -1.8938704947230707, 0.8951597690946617),
                Biquad(1.0, -2.0, 1.0, -1.9946144559930215, 0.9946217070140843),
            )
            else -> arrayOf(
                Biquad(0.25558782037851757, 0.5111756407570351, 0.25558782037851757, -0.14053608242071078, 0.0049375976155402),
                Biquad(1.0, -2.0, 1.0, -1.884901217428792, 0.8864214718161674),
                Biquad(1.0, -2.0, 1.0, -1.9941388812663283, 0.9941474694445309),
            )
        }

    fun process(sample: Double): Double {
        require(sample.isFinite()) { "Non-finite PCM sample" }
        var output = sample
        for (section in sections) output = section.process(output)
        return output
    }

    fun reset() {
        sections.forEach { it.reset() }
    }

    private class Biquad(
        private val b0: Double,
        private val b1: Double,
        private val b2: Double,
        private val a1: Double,
        private val a2: Double,
    ) {
        // Transposed direct form II; each section owns its own carried state.
        private var s1 = 0.0
        private var s2 = 0.0

        fun process(x: Double): Double {
            val y = b0 * x + s1
            val nextS1 = b1 * x - a1 * y + s2
            val nextS2 = b2 * x - a2 * y
            s1 = nextS1
            s2 = nextS2
            return y
        }

        fun reset() {
            s1 = 0.0
            s2 = 0.0
        }
    }
}
