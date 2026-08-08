package com.blakelabs.guitartuner.audio

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuitarPitchMatcherTest {
    private val standard = listOf(
        candidate("E2", 40),
        candidate("A2", 45),
        candidate("D3", 50),
        candidate("G3", 55),
        candidate("B3", 59),
        candidate("E4", 64),
    )

    @Test
    fun `maps low E fundamental to E2`() {
        val match = GuitarPitchMatcher.match(82.4069, standard, maxDistanceCents = 180.0)

        assertNotNull(match)
        assertEquals("E2", match!!.label)
        assertEquals(1, match.harmonic)
        assertTrue(abs(match.normalizedFrequencyHz - 82.4069) < 0.05)
    }

    @Test
    fun `folds low E second harmonic back to E2 instead of D3`() {
        val lowESecondHarmonic = 82.4069 * 2.0
        val match = GuitarPitchMatcher.match(lowESecondHarmonic, standard, maxDistanceCents = 180.0)

        assertNotNull(match)
        assertEquals("E2", match!!.label)
        assertEquals(2, match.harmonic)
        assertTrue(abs(match.normalizedFrequencyHz - 82.4069) < 0.05)
        assertTrue(abs(match.cents) < 1.0)
    }

    @Test
    fun `keeps actual D3 as D3`() {
        val match = GuitarPitchMatcher.match(146.8324, standard, maxDistanceCents = 180.0)

        assertNotNull(match)
        assertEquals("D3", match!!.label)
        assertEquals(1, match.harmonic)
    }

    @Test
    fun `rejects pitch too far from guitar string or second harmonic`() {
        val match = GuitarPitchMatcher.match(72.0, standard, maxDistanceCents = 180.0)
        assertEquals(null, match)
    }

    private fun candidate(label: String, midi: Int) = GuitarPitchMatcher.Candidate(
        label = label,
        fundamentalHz = MusicTheory.frequencyForMidi(midi),
    )
}
