package com.blakelabs.guitartuner.audio

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object MusicTheory {
    private val noteNames = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

    data class Note(
        val midi: Int,
        val name: String,
        val octave: Int,
        val frequencyHz: Double,
        val cents: Double,
    ) {
        val label: String get() = "$name$octave"
    }

    fun frequencyForMidi(midi: Int, a4Hz: Double = 440.0): Double =
        a4Hz * 2.0.pow((midi - 69) / 12.0)

    fun nearestNote(frequencyHz: Double, a4Hz: Double = 440.0): Note {
        require(frequencyHz > 0.0) { "frequencyHz must be positive" }
        require(a4Hz > 0.0) { "a4Hz must be positive" }

        val midiFloat = 69.0 + 12.0 * log2(frequencyHz / a4Hz)
        val midi = midiFloat.roundToInt()
        val target = frequencyForMidi(midi, a4Hz)
        return noteFromMidi(
            midi = midi,
            a4Hz = a4Hz,
            cents = centsBetween(frequencyHz, target),
        )
    }

    fun noteFromMidi(midi: Int, a4Hz: Double = 440.0, cents: Double = 0.0): Note {
        val noteIndex = ((midi % 12) + 12) % 12
        val octave = midi / 12 - 1
        return Note(
            midi = midi,
            name = noteNames[noteIndex],
            octave = octave,
            frequencyHz = frequencyForMidi(midi, a4Hz),
            cents = cents,
        )
    }

    fun centsBetween(frequencyHz: Double, targetHz: Double): Double {
        require(frequencyHz > 0.0) { "frequencyHz must be positive" }
        require(targetHz > 0.0) { "targetHz must be positive" }
        return 1200.0 * log2(frequencyHz / targetHz)
    }

    private fun log2(value: Double): Double = ln(value) / ln(2.0)
}
