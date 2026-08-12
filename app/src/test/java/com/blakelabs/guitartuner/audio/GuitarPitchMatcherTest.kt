package com.blakelabs.guitartuner.audio

import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun `folds low E second harmonic back to E2 across realistic drift`() {
        listOf(-12.0, 0.0, 12.0).forEach { cents ->
            val harmonic = 82.4069 * 2.0 * 2.0.pow(cents / 1200.0)
            val match = GuitarPitchMatcher.match(harmonic, standard, maxDistanceCents = 180.0)

            assertNotNull("No match at $cents cents", match)
            assertEquals("E2", match!!.label)
            assertEquals(2, match.harmonic)
            assertTrue(abs(MusicTheory.centsBetween(match.normalizedFrequencyHz, 82.4069) - cents) < 0.1)
        }
    }

    @Test
    fun `prefers a real D3 fundamental over Drop D second harmonic`() {
        val dropD = listOf(
            candidate("D2", 38),
            candidate("A2", 45),
            candidate("D3", 50),
            candidate("G3", 55),
            candidate("B3", 59),
            candidate("E4", 64),
        )

        val match = GuitarPitchMatcher.match(146.8324, dropD, maxDistanceCents = 180.0)

        assertNotNull(match)
        assertEquals("D3", match!!.label)
        assertEquals(1, match.harmonic)
    }

    @Test
    fun `rejects pitch outside every string neighborhood`() {
        assertNull(GuitarPitchMatcher.match(72.0, standard, maxDistanceCents = 180.0))
    }

    @Test
    fun `rejects non finite measurements and invalid tolerance`() {
        assertNull(GuitarPitchMatcher.match(Double.NaN, standard, maxDistanceCents = 180.0))
        assertThrows(IllegalArgumentException::class.java) {
            GuitarPitchMatcher.match(82.4069, standard, maxDistanceCents = -1.0)
        }
    }

    private fun candidate(label: String, midi: Int) = GuitarPitchMatcher.Candidate(
        label = label,
        fundamentalHz = MusicTheory.frequencyForMidi(midi),
    )
}
