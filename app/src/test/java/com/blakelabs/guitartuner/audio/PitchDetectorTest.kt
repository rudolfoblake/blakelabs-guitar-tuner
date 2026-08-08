package com.blakelabs.guitartuner.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            assertTrue(
                "Expected $expected Hz but got ${result.frequencyHz} Hz ($cents cents)",
                kotlin.math.abs(cents) < 1.5,
            )
        }
    }

    @Test
    fun `detects low level harmonic rich guitar signals`() {
        val frequencies = listOf(82.4069, 146.8324, 329.6276)

        frequencies.forEach { expected ->
            val result = detector.detect(guitarLikeWave(expected))
            assertNotNull("No pitch detected for harmonic-rich $expected Hz", result)
            val cents = MusicTheory.centsBetween(result!!.frequencyHz.toDouble(), expected)
            assertTrue(
                "Expected $expected Hz but got ${result.frequencyHz} Hz ($cents cents)",
                kotlin.math.abs(cents) < 4.0,
            )
        }
    }

    @Test
    fun `rejects silence`() {
        assertNull(detector.detect(ShortArray(8192)))
    }

    private fun sineWave(frequency: Double, size: Int = 8192): ShortArray =
        ShortArray(size) { index ->
            val phase = 2.0 * PI * frequency * index / sampleRate
            (sin(phase) * Short.MAX_VALUE * 0.65).toInt().toShort()
        }

    /**
     * A deterministic plucked-string-ish signal: the second harmonic is intentionally stronger
     * than the fundamental, the amplitude is modest, and a tiny unrelated tone simulates room/mic
     * contamination. This is much closer to the failure mode of a real acoustic guitar than a
     * laboratory sine wave.
     */
    private fun guitarLikeWave(
        frequency: Double,
        size: Int = 8192,
        amplitude: Double = 0.02,
    ): ShortArray {
        val harmonics = doubleArrayOf(0.55, 1.0, 0.65, 0.35, 0.20)
        val normalization = harmonics.sum()

        return ShortArray(size) { index ->
            val time = index.toDouble() / sampleRate
            var signal = 0.0
            harmonics.forEachIndexed { harmonicIndex, weight ->
                val harmonic = harmonicIndex + 1
                signal += weight * sin(
                    2.0 * PI * frequency * harmonic * time + harmonic * 0.17,
                )
            }

            val decay = exp(-time * 1.6)
            val roomNoise = sin(2.0 * PI * 997.0 * time) * 0.00025
            val normalized = ((signal / normalization) * amplitude * decay + roomNoise)
                .coerceIn(-1.0, 1.0)

            (normalized * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
