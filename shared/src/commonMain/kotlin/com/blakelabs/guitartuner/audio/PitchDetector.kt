package com.blakelabs.guitartuner.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * YIN pitch detector tuned for monophonic guitar input.
 *
 * The detector lives in commonMain so Android and iOS execute the exact same DSP implementation.
 * It is dependency-free and reuses its scratch buffers to keep allocations out of the hot path.
 */
class PitchDetector(
    private val sampleRate: Int,
    private val minFrequencyHz: Float = 55f,
    private val maxFrequencyHz: Float = 1200f,
    private val yinThreshold: Float = 0.18f,
    private val rmsGate: Float = 0.0015f,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(minFrequencyHz > 0f) { "minFrequencyHz must be positive" }
        require(maxFrequencyHz > minFrequencyHz) { "maxFrequencyHz must exceed minFrequencyHz" }
        require(yinThreshold in 0f..1f) { "yinThreshold must be between zero and one" }
        require(rmsGate >= 0f) { "rmsGate must not be negative" }
    }

    data class Result(
        val frequencyHz: Float,
        val confidence: Float,
        val rms: Float,
    )

    private var normalized = FloatArray(0)
    private var difference = FloatArray(0)
    private var cmndf = FloatArray(0)

    fun detect(samples: ShortArray): Result? {
        if (samples.size < MIN_ANALYSIS_SAMPLES) return null

        ensureNormalizedCapacity(samples.size)
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

        ensureTauCapacity(maxTau + 1)
        difference[0] = 0f
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
            tau = minTau
            for (candidate in minTau + 1..maxTau) {
                if (cmndf[candidate] < cmndf[tau]) tau = candidate
            }
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

    private fun ensureNormalizedCapacity(size: Int) {
        if (normalized.size < size) normalized = FloatArray(size)
    }

    private fun ensureTauCapacity(size: Int) {
        if (difference.size < size) difference = FloatArray(size)
        if (cmndf.size < size) cmndf = FloatArray(size)
    }

    private companion object {
        const val MIN_ANALYSIS_SAMPLES = 2048
        const val FALLBACK_MAX_CMND = 0.45f
        const val EPSILON = 1e-12
    }
}
