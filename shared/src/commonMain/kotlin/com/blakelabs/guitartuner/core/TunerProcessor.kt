package com.blakelabs.guitartuner.core

import com.blakelabs.guitartuner.audio.GuitarPitchMatcher
import com.blakelabs.guitartuner.audio.MusicTheory
import com.blakelabs.guitartuner.audio.PitchDetector
import kotlin.math.abs

/** Swift-friendly immutable result from one accepted tuner analysis frame. */
data class TunerFrame(
    val noteLabel: String,
    val frequencyHz: Double,
    val targetHz: Double,
    val cents: Double,
    val confidence: Float,
    val rms: Float,
    val status: Int,
)

/**
 * Platform-neutral tuner state machine used by iOS and available to future Android refactors.
 *
 * Integer mode/preset values keep the Objective-C/Swift bridge intentionally small:
 * mode 0 = guitar, 1 = chromatic; preset 0 = Standard, 1 = Drop D, 2 = DADGAD.
 */
class TunerProcessor(sampleRate: Int) {
    private data class GuitarString(val label: String, val midi: Int)
    private data class Measurement(
        val label: String,
        val targetHz: Double,
        val fundamentalHz: Double,
    )

    private val detector = PitchDetector(sampleRate)
    private val frequencyHistory = mutableListOf<Double>()
    private var currentLabel: String? = null
    private var pendingTargetLabel: String? = null
    private var pendingTargetHits = 0
    private var misses = 0

    fun reset() {
        frequencyHistory.clear()
        currentLabel = null
        pendingTargetLabel = null
        pendingTargetHits = 0
        misses = 0
    }

    fun analyze(
        samples: ShortArray,
        mode: Int,
        preset: Int,
        selectedStringIndex: Int,
        a4Hz: Double,
    ): TunerFrame? {
        val result = detector.detect(samples) ?: return registerMiss()
        val safeA4 = a4Hz.coerceIn(MIN_A4_HZ, MAX_A4_HZ)
        val measurement = when (mode) {
            MODE_CHROMATIC -> resolveChromatic(result.frequencyHz.toDouble(), safeA4)
            else -> resolveGuitar(
                detectedHz = result.frequencyHz.toDouble(),
                preset = preset,
                selectedStringIndex = selectedStringIndex,
                a4Hz = safeA4,
            )
        } ?: return registerMiss()

        val requiredConfidence = if (mode != MODE_CHROMATIC && measurement.targetHz <= LOW_STRING_MAX_HZ) {
            LOW_STRING_MIN_ACCEPTED_CONFIDENCE
        } else {
            MIN_ACCEPTED_CONFIDENCE
        }
        if (result.confidence < requiredConfidence) return registerMiss()

        val manualTarget = mode != MODE_CHROMATIC && selectedStringIndex >= 0
        if (!confirmTarget(measurement.label, manualTarget)) return null

        if (currentLabel != null && currentLabel != measurement.label) frequencyHistory.clear()
        currentLabel = measurement.label
        misses = 0
        pushFrequency(measurement.fundamentalHz)

        val stableFrequency = median(frequencyHistory)
        val cents = MusicTheory.centsBetween(stableFrequency, measurement.targetHz)
        val status = when {
            abs(cents) <= IN_TUNE_CENTS && result.confidence >= IN_TUNE_CONFIDENCE -> STATUS_IN_TUNE
            cents < 0.0 -> STATUS_FLAT
            else -> STATUS_SHARP
        }

        return TunerFrame(
            noteLabel = measurement.label,
            frequencyHz = stableFrequency,
            targetHz = measurement.targetHz,
            cents = cents.coerceIn(-DISPLAY_CENTS_LIMIT, DISPLAY_CENTS_LIMIT),
            confidence = result.confidence,
            rms = result.rms,
            status = status,
        )
    }

    private fun resolveChromatic(detectedHz: Double, a4Hz: Double): Measurement {
        val nearest = MusicTheory.nearestNote(detectedHz, a4Hz)
        return Measurement(nearest.label, nearest.frequencyHz, detectedHz)
    }

    private fun resolveGuitar(
        detectedHz: Double,
        preset: Int,
        selectedStringIndex: Int,
        a4Hz: Double,
    ): Measurement? {
        val strings = stringsForPreset(preset)
        val source = if (selectedStringIndex in strings.indices) {
            listOf(strings[selectedStringIndex])
        } else {
            strings
        }
        val candidates = source.map { string ->
            GuitarPitchMatcher.Candidate(
                label = string.label,
                fundamentalHz = MusicTheory.frequencyForMidi(string.midi, a4Hz),
            )
        }
        val match = GuitarPitchMatcher.match(
            detectedHz = detectedHz,
            candidates = candidates,
            maxDistanceCents = if (selectedStringIndex in strings.indices) {
                GUITAR_MANUAL_MATCH_MAX_CENTS
            } else {
                GUITAR_AUTO_MATCH_MAX_CENTS
            },
        ) ?: return null

        return Measurement(match.label, match.targetHz, match.normalizedFrequencyHz)
    }

    private fun stringsForPreset(preset: Int): List<GuitarString> = when (preset) {
        PRESET_DROP_D -> listOf(
            GuitarString("D2", 38), GuitarString("A2", 45), GuitarString("D3", 50),
            GuitarString("G3", 55), GuitarString("B3", 59), GuitarString("E4", 64),
        )
        PRESET_DADGAD -> listOf(
            GuitarString("D2", 38), GuitarString("A2", 45), GuitarString("D3", 50),
            GuitarString("G3", 55), GuitarString("A3", 57), GuitarString("D4", 62),
        )
        else -> listOf(
            GuitarString("E2", 40), GuitarString("A2", 45), GuitarString("D3", 50),
            GuitarString("G3", 55), GuitarString("B3", 59), GuitarString("E4", 64),
        )
    }

    private fun confirmTarget(label: String, manualTarget: Boolean): Boolean {
        if (manualTarget) {
            clearPendingTarget()
            return true
        }
        if (currentLabel == label) {
            clearPendingTarget()
            return true
        }

        if (pendingTargetLabel == label) {
            pendingTargetHits++
        } else {
            pendingTargetLabel = label
            pendingTargetHits = 1
        }

        val requiredHits = if (currentLabel == null) INITIAL_TARGET_CONFIRM_FRAMES else TARGET_SWITCH_CONFIRM_FRAMES
        if (pendingTargetHits < requiredHits) return false
        clearPendingTarget()
        return true
    }

    private fun registerMiss(): TunerFrame? {
        misses++
        clearPendingTarget()
        if (misses >= MISSES_BEFORE_CLEAR) {
            frequencyHistory.clear()
            currentLabel = null
        }
        return null
    }

    private fun clearPendingTarget() {
        pendingTargetLabel = null
        pendingTargetHits = 0
    }

    private fun pushFrequency(value: Double) {
        frequencyHistory += value
        while (frequencyHistory.size > HISTORY_SIZE) frequencyHistory.removeAt(0)
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    companion object {
        const val MODE_GUITAR = 0
        const val MODE_CHROMATIC = 1
        const val PRESET_STANDARD = 0
        const val PRESET_DROP_D = 1
        const val PRESET_DADGAD = 2
        const val STATUS_WAITING = 0
        const val STATUS_FLAT = 1
        const val STATUS_IN_TUNE = 2
        const val STATUS_SHARP = 3

        private const val HISTORY_SIZE = 5
        private const val MISSES_BEFORE_CLEAR = 8
        private const val INITIAL_TARGET_CONFIRM_FRAMES = 2
        private const val TARGET_SWITCH_CONFIRM_FRAMES = 3
        private const val MIN_ACCEPTED_CONFIDENCE = 0.55f
        private const val LOW_STRING_MIN_ACCEPTED_CONFIDENCE = 0.48f
        private const val LOW_STRING_MAX_HZ = 120.0
        private const val IN_TUNE_CONFIDENCE = 0.65f
        private const val IN_TUNE_CENTS = 3.0
        private const val DISPLAY_CENTS_LIMIT = 50.0
        private const val GUITAR_AUTO_MATCH_MAX_CENTS = 180.0
        private const val GUITAR_MANUAL_MATCH_MAX_CENTS = 450.0
        private const val MIN_A4_HZ = 430.0
        private const val MAX_A4_HZ = 450.0
    }
}
