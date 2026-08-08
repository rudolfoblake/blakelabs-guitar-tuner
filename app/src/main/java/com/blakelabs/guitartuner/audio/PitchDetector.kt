package com.blakelabs.guitartuner.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * YIN pitch detector tuned for monophonic guitar input.
 *
 * The implementation intentionally stays dependency-free so the signal path is auditable,
 * offline, and boring in the best possible way. No SDK gets to listen over our shoulder.
 */
class PitchDetector(
    private val sampleRate: Int,
    private val minFrequencyHz: Float = 55f,
    private val maxFrequencyHz: Float = 1200f,
    private val yinThreshold: Float = 0.18f,
    private val rmsGate: Float = 0.0015f,
) {
    data class Result(
        val frequencyHz: Float,
        val confidence: Float,
        val rms: Float,
    )

    fun detect(samples: ShortArray): Result? {
        if (samples.size < MIN_ANALYSIS_SAMPLES) return null

        val normalized = FloatArray(samples.size)
        var mean = 0.0
        for (sample in samples) mean += sample.toDouble()
        mean /= samples.size

        var energy = 0.0
        for (i in samples.indices) {
            val value = ((samples[i] - mean) / Short.MAX_VALUE).toFloat()
            normalized[i] = value
            energy += value * value
        }

        val rms = sqrt(energy / samples.size).toFloat()
        if (rms < rmsGate) return null

        val minTau = max(2, (sampleRate / maxFrequencyHz).toInt())
        val maxTau = (sampleRate / minFrequencyHz)
            .toInt()
            .coerceAtMost(samples.size / 2 - 1)
        if (maxTau <= minTau + 2) return null

        val difference = FloatArray(maxTau + 1)
        val comparisonLength = samples.size - maxTau

        for (tau in 1..maxTau) {
            var sum = 0.0
            var i = 0
            while (i < comparisonLength) {
                val delta = normalized[i] - normalized[i + tau]
                sum += delta * delta
                i++
            }
            difference[tau] = sum.toFloat()
        }

        val cmndf = FloatArray(maxTau + 1)
        cmndf[0] = 1f
        var runningSum = 0.0
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            cmndf[tau] = if (runningSum <= EPSILON) {
                1f
            } else {
                (difference[tau] * tau / runningSum).toFloat()
            }
        }

        var tau = minTau
        while (tau <= maxTau) {
            if (cmndf[tau] < yinThreshold) {
                while (tau + 1 <= maxTau && cmndf[tau + 1] < cmndf[tau]) tau++
                break
            }
            tau++
        }

        if (tau > maxTau) {
            tau = (minTau..maxTau).minByOrNull { cmndf[it] } ?: return null
            // The physical 0.2.1 build proved the relaxed fallback was useful for weak strings,
            // but 0.45 also admitted too much room noise. Keep the normal YIN path permissive
            // while requiring a meaningfully periodic fallback candidate.
            if (cmndf[tau] > FALLBACK_MAX_CMND) return null
        }

        val refinedTau = parabolicInterpolation(cmndf, tau)
        if (refinedTau <= 0f) return null

        val frequency = sampleRate / refinedTau
        if (frequency !in minFrequencyHz..maxFrequencyHz) return null

        return Result(
            frequencyHz = frequency,
            confidence = (1f - cmndf[tau]).coerceIn(0f, 1f),
            rms = rms,
        )
    }

    private fun parabolicInterpolation(values: FloatArray, index: Int): Float {
        if (index <= 0 || index >= values.lastIndex) return index.toFloat()

        val left = values[index - 1]
        val center = values[index]
        val right = values[index + 1]
        val denominator = left - (2f * center) + right
        if (abs(denominator) < 1e-9f) return index.toFloat()

        val offset = 0.5f * (left - right) / denominator
        return index + offset.coerceIn(-1f, 1f)
    }

    private companion object {
        const val MIN_ANALYSIS_SAMPLES = 2048
        const val FALLBACK_MAX_CMND = 0.35f
        const val EPSILON = 1e-12
    }
}
