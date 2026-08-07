package com.blakelabs.guitartuner.audio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectorTest {
    private val sampleRate = 48_000
    private val detector = PitchDetector(sampleRate)

    @Test
    fun `detects standard guitar strings within one cent-ish`() {
        val frequencies = listOf(82.4069, 110.0, 146.8324, 195.9977, 246.9417, 329.6276)

        frequencies.forEach { expected ->
            val result = detector.detect(sineWave(expected))
            assertNotNull("No pitch detected for $expected Hz", result)
            val cents = MusicTheory.centsBetween(result!!.frequencyHz.toDouble(), expected)
            assertTrue("Expected $expected Hz but got ${result.frequencyHz} Hz ($cents cents)", kotlin.math.abs(cents) < 1.5)
        }
    }

    private fun sineWave(frequency: Double, size: Int = 4096): ShortArray =
        ShortArray(size) { index ->
            val phase = 2.0 * PI * frequency * index / sampleRate
            (sin(phase) * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
}
