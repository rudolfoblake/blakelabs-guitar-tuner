package com.blakelabs.guitartuner.audio

import kotlin.math.abs

/**
 * Resolves detector output against configured guitar strings and folds the common second-harmonic
 * octave error back to the fundamental for low strings.
 */
object GuitarPitchMatcher {
    data class Candidate(
        val label: String,
        val fundamentalHz: Double,
    ) {
        init {
            require(label.isNotBlank()) { "label must not be blank" }
            require(fundamentalHz > 0.0) { "fundamentalHz must be positive" }
        }
    }

    data class Match(
        val label: String,
        val targetHz: Double,
        val normalizedFrequencyHz: Double,
        val harmonic: Int,
        val cents: Double,
    )

    fun match(
        detectedHz: Double,
        candidates: List<Candidate>,
        maxDistanceCents: Double,
    ): Match? {
        if (!detectedHz.isFinite() || detectedHz <= 0.0 || candidates.isEmpty()) return null
        require(maxDistanceCents >= 0.0) { "maxDistanceCents must not be negative" }

        var best: Match? = null
        var bestScore = Double.POSITIVE_INFINITY

        for (candidate in candidates) {
            for (harmonic in 1..MAX_HARMONIC) {
                val expectedHz = candidate.fundamentalHz * harmonic
                val cents = MusicTheory.centsBetween(detectedHz, expectedHz)
                val distance = abs(cents)
                if (distance > maxDistanceCents) continue

                val score = distance + if (harmonic == 1) 0.0 else SECOND_HARMONIC_PENALTY_CENTS
                if (score < bestScore) {
                    bestScore = score
                    best = Match(
                        label = candidate.label,
                        targetHz = candidate.fundamentalHz,
                        normalizedFrequencyHz = detectedHz / harmonic,
                        harmonic = harmonic,
                        cents = cents,
                    )
                }
            }
        }

        return best
    }

    private const val MAX_HARMONIC = 2
    private const val SECOND_HARMONIC_PENALTY_CENTS = 6.0
}
