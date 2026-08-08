package com.blakelabs.guitartuner.audio

import kotlin.math.abs

/**
 * Maps a detected pitch to a guitar-string fundamental while tolerating the most common
 * octave error: the detector locking to the second harmonic instead of the fundamental.
 *
 * This stays deliberately small and deterministic. A phone microphone often hears E3
 * (the second harmonic of low E2) more strongly than E2 itself. Comparing only detected
 * pitch against string fundamentals can then mislabel that low-E pluck as D3. Folding a
 * clean second-harmonic match back to its fundamental fixes that without making the tuner
 * guess across arbitrary higher harmonics.
 */
object GuitarPitchMatcher {
    data class Candidate(
        val label: String,
        val fundamentalHz: Double,
    )

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
        if (detectedHz <= 0.0 || candidates.isEmpty()) return null

        var best: Match? = null
        var bestScore = Double.POSITIVE_INFINITY

        for (candidate in candidates) {
            for (harmonic in 1..MAX_HARMONIC) {
                val expectedHz = candidate.fundamentalHz * harmonic
                val cents = MusicTheory.centsBetween(detectedHz, expectedHz)
                val distance = abs(cents)
                if (distance > maxDistanceCents) continue

                // Prefer a true fundamental when two candidates are effectively tied, while
                // still allowing a clearly better second-harmonic explanation to win.
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
