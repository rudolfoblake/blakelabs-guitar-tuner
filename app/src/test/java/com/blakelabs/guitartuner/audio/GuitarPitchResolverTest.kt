package com.blakelabs.guitartuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuitarPitchResolverTest {
    private val standardTargets = listOf(40, 45, 50, 55, 59, 64)
        .map { midi -> MusicTheory.frequencyForMidi(midi) }

    @Test
    fun `maps low E second harmonic back to E2 fundamental`() {
        val e2 = standardTargets[0]
        val result = GuitarPitchResolver.bestMatch(
            frequencyHz = e2 * 2.0,
            targetFrequenciesHz = standardTargets,
        )

        assertEquals(0, result.targetIndex)
        assertTrue(result.usedSecondHarmonic)
        assertEquals(e2, result.normalizedFrequencyHz, 0.02)
    }

    @Test
    fun `does not mistake a real D3 fundamental for low E harmonic`() {
        val d3 = standardTargets[2]
        val result = GuitarPitchResolver.bestMatch(
            frequencyHz = d3,
            targetFrequenciesHz = standardTargets,
        )

        assertEquals(2, result.targetIndex)
        assertTrue(!result.usedSecondHarmonic)
        assertEquals(d3, result.normalizedFrequencyHz, 0.02)
    }

    @Test
    fun `normalizes selected low E when detector reports octave`() {
        val e2 = standardTargets[0]
        val result = GuitarPitchResolver.matchForTarget(
            frequencyHz = e2 * 2.0,
            targetFrequencyHz = e2,
            targetIndex = 0,
        )

        assertTrue(result.usedSecondHarmonic)
        assertTrue(result.scoreCents < 20.0)
        assertEquals(e2, result.normalizedFrequencyHz, 0.02)
    }

    @Test
    fun `keeps high E fundamental as high E`() {
        val e4 = standardTargets[5]
        val result = GuitarPitchResolver.bestMatch(
            frequencyHz = e4,
            targetFrequenciesHz = standardTargets,
        )

        assertEquals(5, result.targetIndex)
        assertTrue(!result.usedSecondHarmonic)
        assertEquals(e4, result.normalizedFrequencyHz, 0.02)
    }
}
