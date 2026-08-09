package com.blakelabs.guitartuner

import androidx.lifecycle.ViewModel
import com.blakelabs.guitartuner.audio.AudioEngine
import com.blakelabs.guitartuner.audio.MusicTheory
import com.blakelabs.guitartuner.audio.PitchDetector
import java.util.ArrayDeque
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val frequencyHistory = ArrayDeque<Double>()
    private var audioEngine: AudioEngine? = null
    private var misses = 0

    fun start() {
        if (_state.value.listening) return

        _state.value = _state.value.copy(
            listening = true,
            error = null,
        )
        misses = 0

        audioEngine = AudioEngine(
            onPitch = ::handlePitch,
            onSignal = ::handleSignal,
            onError = { message ->
                _state.value = _state.value.copy(
                    listening = false,
                    error = message,
                    frequencyHz = null,
                    targetHz = null,
                    noteLabel = "—",
                    confidence = 0f,
                    status = PitchStatus.WAITING,
                )
            },
        ).also(AudioEngine::start)
    }

    fun stop() {
        audioEngine?.stop()
        audioEngine = null
        frequencyHistory.clear()
        misses = 0
        _state.value = _state.value.copy(
            listening = false,
            frequencyHz = null,
            targetHz = null,
            noteLabel = "—",
            cents = 0.0,
            confidence = 0f,
            signal = 0f,
            status = PitchStatus.WAITING,
        )
    }

    fun setMode(mode: Mode) {
        resetTracking()
        _state.value = _state.value.copy(
            mode = mode,
            selectedStringIndex = null,
        )
    }

    fun setPreset(preset: TuningPreset) {
        resetTracking()
        _state.value = _state.value.copy(
            preset = preset,
            selectedStringIndex = null,
        )
    }

    fun selectString(index: Int?) {
        resetTracking()
        _state.value = _state.value.copy(selectedStringIndex = index)
    }

    fun adjustA4(delta: Double) {
        resetTracking()
        _state.value = _state.value.copy(
            a4Hz = (_state.value.a4Hz + delta).coerceIn(MIN_A4_HZ, MAX_A4_HZ),
        )
    }

    private fun handleSignal(rms: Float) {
        if (!_state.value.listening) return
        _state.value = _state.value.copy(signal = signalLevel(rms))
    }

    private fun handlePitch(result: PitchDetector.Result?) {
        if (!_state.value.listening) return

        if (result == null || result.confidence < MIN_ACCEPTED_CONFIDENCE) {
            misses++
            if (misses >= MISSES_BEFORE_CLEAR) {
                frequencyHistory.clear()
                _state.value = _state.value.copy(
                    frequencyHz = null,
                    targetHz = null,
                    noteLabel = "—",
                    cents = 0.0,
                    confidence = result?.confidence ?: 0f,
                    status = PitchStatus.WAITING,
                )
            } else {
                _state.value = _state.value.copy(
                    confidence = result?.confidence ?: 0f,
                )
            }
            return
        }

        misses = 0
        pushFrequency(result.frequencyHz.toDouble())
        val stableFrequency = median(frequencyHistory)
        val fresh = _state.value

        val target = when (fresh.mode) {
            Mode.CHROMATIC -> {
                val nearest = MusicTheory.nearestNote(stableFrequency, fresh.a4Hz)
                Target(nearest.label, nearest.frequencyHz)
            }

            Mode.GUITAR -> guitarTarget(fresh, stableFrequency)
        }

        val cents = MusicTheory.centsBetween(stableFrequency, target.frequencyHz)
        val status = when {
            abs(cents) <= IN_TUNE_CENTS && result.confidence >= IN_TUNE_CONFIDENCE -> PitchStatus.IN_TUNE
            cents < 0.0 -> PitchStatus.FLAT
            else -> PitchStatus.SHARP
        }

        _state.value = fresh.copy(
            frequencyHz = stableFrequency,
            targetHz = target.frequencyHz,
            noteLabel = target.label,
            cents = cents.coerceIn(-DISPLAY_CENTS_LIMIT, DISPLAY_CENTS_LIMIT),
            confidence = result.confidence,
            status = status,
            error = null,
        )
    }

    private fun guitarTarget(state: UiState, frequencyHz: Double): Target {
        val strings = state.preset.strings
        val selected = state.selectedStringIndex
        val targetString = if (selected != null && selected in strings.indices) {
            strings[selected]
        } else {
            strings.minBy { string ->
                val targetHz = MusicTheory.frequencyForMidi(string.midi, state.a4Hz)
                abs(MusicTheory.centsBetween(frequencyHz, targetHz))
            }
        }

        return Target(
            label = targetString.label,
            frequencyHz = MusicTheory.frequencyForMidi(targetString.midi, state.a4Hz),
        )
    }

    private fun resetTracking() {
        frequencyHistory.clear()
        misses = 0
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

    override fun onCleared() {
        audioEngine?.stop()
        audioEngine = null
        super.onCleared()
    }

    private companion object {
        const val HISTORY_SIZE = 5
        const val MISSES_BEFORE_CLEAR = 7
        const val MIN_ACCEPTED_CONFIDENCE = 0.45f
        const val IN_TUNE_CONFIDENCE = 0.60f
        const val IN_TUNE_CENTS = 3.0
        const val DISPLAY_CENTS_LIMIT = 50.0
        const val MIN_A4_HZ = 430.0
        const val MAX_A4_HZ = 450.0
        const val SIGNAL_FLOOR = 0.0005f
        const val SIGNAL_RANGE = 0.025f
    }
}
