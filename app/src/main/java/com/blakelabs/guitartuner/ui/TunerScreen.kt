package com.blakelabs.guitartuner.ui

import androidx.compose.runtime.Composable
import com.blakelabs.guitartuner.TunerViewModel

/** Public entry point kept stable while the premium UI evolves independently. */
@Composable
fun TunerScreen(
    state: TunerViewModel.UiState,
    microphoneGranted: Boolean,
    shouldListen: Boolean,
    onRequestMicrophone: () -> Unit,
    onListeningChange: (Boolean) -> Unit,
    onModeChange: (TunerViewModel.Mode) -> Unit,
    onPresetChange: (TunerViewModel.TuningPreset) -> Unit,
    onStringSelected: (Int?) -> Unit,
    onA4Change: (Double) -> Unit,
) {
    PremiumTunerScreen(
        state = state,
        microphoneGranted = microphoneGranted,
        shouldListen = shouldListen,
        onRequestMicrophone = onRequestMicrophone,
        onListeningChange = onListeningChange,
        onModeChange = onModeChange,
        onPresetChange = onPresetChange,
        onStringSelected = onStringSelected,
        onA4Change = onA4Change,
    )
}
