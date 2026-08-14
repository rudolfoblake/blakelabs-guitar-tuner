package com.blakelabs.guitartuner.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TunerProcessorTest {
    @Test
    fun standardLowEIsDetected() {
        val processor = TunerProcessor(48_000)
        val samples = sine(82.4069, 48_000, 4096)

        var frame: TunerFrame? = null
        repeat(3) {
            frame = processor.analyze(samples, 0, 0, -1, 440.0) ?: frame
        }

        val detected = assertNotNull(frame)
        assertEquals("E2", detected.noteLabel)
        assertTrue(abs(detected.cents) < 2.0)
    }

    @Test
    fun chromaticA4IsDetected() {
        val processor = TunerProcessor(48_000)
        val samples = sine(440.0, 48_000, 4096)

        var frame: TunerFrame? = null
        repeat(3) {
            frame = processor.analyze(samples, 1, 0, -1, 440.0) ?: frame
        }

        val detected = assertNotNull(frame)
        assertEquals("A4", detected.noteLabel)
        assertTrue(abs(detected.cents) < 2.0)
    }

    private fun sine(frequency: Double, sampleRate: Int, size: Int): ShortArray =
        ShortArray(size) { index ->
            (sin(2.0 * PI * frequency * index / sampleRate) * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
}
