package com.blakelabs.guitartuner.audio

import kotlin.math.abs

/**
 * Maps a detected periodic frequency to one of the expected guitar strings.
 *
 * Phone microphones and plucked guitar strings often make the second harmonic louder than the
 * fundamental, especially on the low E string. YIN can therefore return ~164.8 Hz for an E2
 * instead of ~82.4 Hz. In guitar mode we know the small set of valid string targets, so we can
 * safely compare both the detected frequency and its half-frequency against those targets.
 */
object GuitarPitchResolver {
    data class Match(
        val targetIndex: Int,
        val normalizedFrequencyHz: Double,
        val scoreCents: Double,
        val usedSecondHarmonic: Boolean,
    )

    fun bestMatch(
        frequencyHz: Double,
        targetFrequenciesHz: List<Double>,
    ): Match {
        require(frequencyHz > 0.0) { "frequencyHz must be positive" }
        require(targetFrequenciesHz.isNotEmpty()) { "targetFrequenciesHz must not be empty" }

        return targetFrequenciesHz.indices
            .map { index -> matchForTarget(frequencyHz, targetFrequenciesHz[index], index) }
            .minBy { it.scoreCents }
    }

    fun matchForTarget(
        frequencyHz: Double,
        targetFrequencyHz: Double,
        targetIndex: Int,
    ): Match {
        require(frequencyHz > 0.0) { "frequencyHz must be positive" }
        require(targetFrequencyHz > 0.0) { "targetFrequencyHz must be positive" }

        val fundamentalError = abs(MusicTheory.centsBetween(frequencyHz, targetFrequencyHz))
        val halfFrequency = frequencyHz / 2.0
        val harmonicError = if (halfFrequency >= MIN_NORMALIZED_FREQUENCY_HZ) {
            abs(MusicTheory.centsBetween(halfFrequency, targetFrequencyHz)) +
                SECOND_HARMONIC_PENALTY_CENTS
        } else {
            Double.POSITIVE_INFINITY
        }

        return if (harmonicError < fundamentalError) {
            Match(
                targetIndex = targetIndex,
                normalizedFrequencyHz = halfFrequency,
                scoreCents = harmonicError,
                usedSecondHarmonic = true,
            )
        } else {
            Match(
                targetIndex = targetIndex,
                normalizedFrequencyHz = frequencyHz,
                scoreCents = fundamentalError,
                usedSecondHarmonic = false,
            )
        }
    }

    private const val SECOND_HARMONIC_PENALTY_CENTS = 18.0
    private const val MIN_NORMALIZED_FREQUENCY_HZ = 55.0
}
