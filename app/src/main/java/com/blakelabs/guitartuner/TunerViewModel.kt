package com.blakelabs.guitartuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blakelabs.guitartuner.audio.AudioEngine
import com.blakelabs.guitartuner.audio.GuitarPitchMatcher
import com.blakelabs.guitartuner.audio.MusicTheory
import com.blakelabs.guitartuner.audio.PitchDetector
import java.util.ArrayDeque
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TunerViewModel : ViewModel() {
    enum class Mode { GUITAR, CHROMATIC }

    enum class PitchStatus {
        WAITING,
        FLAT,
        IN_TUNE,
        SHARP,
    }

    data class GuitarString(
        val label: String,
        val midi: Int,
    )

    enum class TuningPreset(
        val label: String,
        val strings: List<GuitarString>,
    ) {
        STANDARD(
            "Standard",
            listOf(
                GuitarString("E2", 40),
                GuitarString("A2", 45),
                GuitarString("D3", 50),
                GuitarString("G3", 55),
                GuitarString("B3", 59),
                GuitarString("E4", 64),
            ),
        ),
        DROP_D(
            "Drop D",
            listOf(
                GuitarString("D2", 38),
                GuitarString("A2", 45),
                GuitarString("D3", 50),
                GuitarString("G3", 55),
                GuitarString("B3", 59),
                GuitarString("E4", 64),
            ),
        ),
        DADGAD(
            "DADGAD",
            listOf(
                GuitarString("D2", 38),
                GuitarString("A2", 45),
                GuitarString("D3", 50),
                GuitarString("G3", 55),
                GuitarString("A3", 57),
                GuitarString("D4", 62),
            ),
        ),
    }

    data class UiState(
        val listening: Boolean = false,
        val mode: Mode = Mode.GUITAR,
        val preset: TuningPreset = TuningPreset.STANDARD,
        val selectedStringIndex: Int? = null,
        val a4Hz: Double = 440.0,
        val frequencyHz: Double? = null,
        val targetHz: Double? = null,
        val noteLabel: String = "—",
        val cents: Double = 0.0,
        val confidence: Float = 0f,
        val signal: Float = 0f,
        val status: PitchStatus = PitchStatus.WAITING,
        val error: String? = null,
    ) {
        val isInTune: Boolean get() = status == PitchStatus.IN_TUNE
    }

    private data class Target(
        val label: String,
        val frequencyHz: Double,
    )

    private data class Measurement(
        val target: Target,
        val fundamentalFrequencyHz: Double,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val frequencyHistory = ArrayDeque<Double>()
    private var audioEngine: AudioEngine? = null
    private var audioSession = 0L
    private var misses = 0
    private var pendingTargetLabel: String? = null
    private var pendingTargetHits = 0

    fun start() {
        if (_state.value.listening || audioEngine != null) return

        resetTracking()
        val session = ++audioSession
        _state.update { current ->
            clearMeasurement(current).copy(
                listening = true,
                signal = 0f,
                error = null,
            )
        }

        val engine = AudioEngine(
            onPitch = { result -> dispatchAudioCallback(session) { handlePitch(result) } },
            onSignal = { rms -> dispatchAudioCallback(session) { handleSignal(rms) } },
            onError = { message -> dispatchAudioCallback(session) { handleAudioError(message) } },
        )
        audioEngine = engine
        engine.start()
    }

    fun stop() {
        val engine = audioEngine
        audioSession++
        audioEngine = null
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(
                listening = false,
                signal = 0f,
            )
        }
        engine?.stop()
    }

    fun setMode(mode: Mode) {
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(
                mode = mode,
                selectedStringIndex = null,
            )
        }
    }

    fun setPreset(preset: TuningPreset) {
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(
                preset = preset,
                selectedStringIndex = null,
            )
        }
    }

    fun selectString(index: Int?) {
        require(index == null || index in _state.value.preset.strings.indices) {
            "selected string index is outside the current preset"
        }
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(selectedStringIndex = index)
        }
    }

    fun adjustA4(delta: Double) {
        if (!delta.isFinite()) return
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(
                a4Hz = (current.a4Hz + delta).coerceIn(MIN_A4_HZ, MAX_A4_HZ),
            )
        }
    }

    private fun dispatchAudioCallback(session: Long, callback: () -> Unit) {
        viewModelScope.launch {
            if (session == audioSession) callback()
        }
    }

    private fun handleSignal(rms: Float) {
        if (!_state.value.listening) return
        _state.update { current -> current.copy(signal = signalLevel(rms)) }
    }

    private fun handleAudioError(message: String) {
        audioEngine = null
        resetTracking()
        _state.update { current ->
            clearMeasurement(current).copy(
                listening = false,
                signal = 0f,
                error = message,
            )
        }
    }

    private fun handlePitch(result: PitchDetector.Result?) {
        if (!_state.value.listening) return

        if (result == null) {
            registerMiss(0f)
            return
        }

        val state = _state.value
        val measurement = resolveMeasurement(state, result.frequencyHz.toDouble())
        if (measurement == null) {
            registerMiss(result.confidence)
            return
        }

        val requiredConfidence = when {
            state.mode == Mode.GUITAR && measurement.target.frequencyHz <= LOW_STRING_MAX_HZ ->
                LOW_STRING_MIN_ACCEPTED_CONFIDENCE
            else -> MIN_ACCEPTED_CONFIDENCE
        }
        if (result.confidence < requiredConfidence) {
            registerMiss(result.confidence)
            return
        }

        if (!confirmTarget(measurement.target.label, state)) {
            _state.update { current -> current.copy(confidence = result.confidence) }
            return
        }

        val switchingTarget = state.frequencyHz != null && state.noteLabel != measurement.target.label
        if (switchingTarget) frequencyHistory.clear()

        misses = 0
        pushFrequency(measurement.fundamentalFrequencyHz)
        val stableFrequency = median(frequencyHistory)
        val cents = MusicTheory.centsBetween(stableFrequency, measurement.target.frequencyHz)
        val status = when {
            abs(cents) <= IN_TUNE_CENTS && result.confidence >= IN_TUNE_CONFIDENCE -> PitchStatus.IN_TUNE
            cents < 0.0 -> PitchStatus.FLAT
            else -> PitchStatus.SHARP
        }

        _state.update { current ->
            current.copy(
                frequencyHz = stableFrequency,
                targetHz = measurement.target.frequencyHz,
                noteLabel = measurement.target.label,
                cents = cents.coerceIn(-DISPLAY_CENTS_LIMIT, DISPLAY_CENTS_LIMIT),
                confidence = result.confidence,
                status = status,
                error = null,
            )
        }
    }

    private fun resolveMeasurement(state: UiState, detectedHz: Double): Measurement? {
        return when (state.mode) {
            Mode.CHROMATIC -> {
                val nearest = MusicTheory.nearestNote(detectedHz, state.a4Hz)
                Measurement(
                    target = Target(nearest.label, nearest.frequencyHz),
                    fundamentalFrequencyHz = detectedHz,
                )
            }

            Mode.GUITAR -> {
                val selected = state.selectedStringIndex
                val sourceStrings = if (selected != null && selected in state.preset.strings.indices) {
                    listOf(state.preset.strings[selected])
                } else {
                    state.preset.strings
                }
                val candidates = sourceStrings.map { string ->
                    GuitarPitchMatcher.Candidate(
                        label = string.label,
                        fundamentalHz = MusicTheory.frequencyForMidi(string.midi, state.a4Hz),
                    )
                }
                val match = GuitarPitchMatcher.match(
                    detectedHz = detectedHz,
                    candidates = candidates,
                    maxDistanceCents = if (selected == null) {
                        GUITAR_AUTO_MATCH_MAX_CENTS
                    } else {
                        GUITAR_MANUAL_MATCH_MAX_CENTS
                    },
                ) ?: return null

                Measurement(
                    target = Target(match.label, match.targetHz),
                    fundamentalFrequencyHz = match.normalizedFrequencyHz,
                )
            }
        }
    }

    private fun confirmTarget(label: String, state: UiState): Boolean {
        if (state.mode == Mode.GUITAR && state.selectedStringIndex != null) {
            clearPendingTarget()
            return true
        }

        val currentLabel = state.noteLabel.takeIf { state.frequencyHz != null && it != "—" }
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

        val requiredHits = if (currentLabel == null) {
            INITIAL_TARGET_CONFIRM_FRAMES
        } else {
            TARGET_SWITCH_CONFIRM_FRAMES
        }
        if (pendingTargetHits < requiredHits) return false

        clearPendingTarget()
        return true
    }

    private fun registerMiss(confidence: Float) {
        misses++
        clearPendingTarget()

        if (misses >= MISSES_BEFORE_CLEAR) {
            frequencyHistory.clear()
            _state.update { current ->
                clearMeasurement(current).copy(confidence = confidence)
            }
        } else {
            _state.update { current -> current.copy(confidence = confidence) }
        }
    }

    private fun resetTracking() {
        frequencyHistory.clear()
        misses = 0
        clearPendingTarget()
    }

    private fun clearPendingTarget() {
        pendingTargetLabel = null
        pendingTargetHits = 0
    }

    private fun pushFrequency(value: Double) {
        frequencyHistory.addLast(value)
        while (frequencyHistory.size > HISTORY_SIZE) frequencyHistory.removeFirst()
    }

    private fun median(values: Collection<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun signalLevel(rms: Float): Float =
        ((rms - SIGNAL_FLOOR) / SIGNAL_RANGE).coerceIn(0f, 1f)

    private fun clearMeasurement(state: UiState): UiState = state.copy(
        frequencyHz = null,
        targetHz = null,
        noteLabel = "—",
        cents = 0.0,
        confidence = 0f,
        status = PitchStatus.WAITING,
    )

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private companion object {
        const val HISTORY_SIZE = 5
        const val MISSES_BEFORE_CLEAR = 8
        const val INITIAL_TARGET_CONFIRM_FRAMES = 2
        const val TARGET_SWITCH_CONFIRM_FRAMES = 3
        const val MIN_ACCEPTED_CONFIDENCE = 0.55f
        const val LOW_STRING_MIN_ACCEPTED_CONFIDENCE = 0.48f
        const val LOW_STRING_MAX_HZ = 120.0
        const val IN_TUNE_CONFIDENCE = 0.65f
        const val IN_TUNE_CENTS = 3.0
        const val DISPLAY_CENTS_LIMIT = 50.0
        const val GUITAR_AUTO_MATCH_MAX_CENTS = 180.0
        const val GUITAR_MANUAL_MATCH_MAX_CENTS = 450.0
        const val MIN_A4_HZ = 430.0
        const val MAX_A4_HZ = 450.0
        const val SIGNAL_FLOOR = 0.0005f
        const val SIGNAL_RANGE = 0.025f
    }
}
