package com.blakelabs.guitartuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicTheoryTest {
    @Test
    fun `A4 resolves to 440 Hz and zero cents`() {
        val note = MusicTheory.nearestNote(440.0)

        assertEquals("A4", note.label)
        assertEquals(440.0, note.frequencyHz, 0.0001)
        assertEquals(0.0, note.cents, 0.0001)
    }

    @Test
    fun `standard low E is close to 82 point 41 Hz`() {
        val frequency = MusicTheory.frequencyForMidi(40)

        assertEquals(82.4069, frequency, 0.001)
    }

    @Test
    fun `positive cents means sharp`() {
        assertTrue(MusicTheory.centsBetween(445.0, 440.0) > 0.0)
    }
}
