package com.blakelabs.guitartuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.TunerViewModel

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
    val haptic = LocalHapticFeedback.current
    var previouslyInTune by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hapticsEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(state.isInTune, hapticsEnabled) {
        if (hapticsEnabled && state.isInTune && !previouslyInTune) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previouslyInTune = state.isInTune
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlakeColors.Background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BlakeColors.Primary.copy(alpha = 0.055f),
                            Color.Transparent,
                        ),
                        center = Offset(540f, 300f),
                        radius = 900f,
                    ),
                ),
        )

        if (showSettings) {
            SettingsScreen(
                state = state,
                hapticsEnabled = hapticsEnabled,
                onBack = { showSettings = false },
                onHapticsChange = { hapticsEnabled = it },
                onModeChange = onModeChange,
                onA4Change = onA4Change,
            )
        } else {
            MainTunerScreen(
                state = state,
                microphoneGranted = microphoneGranted,
                shouldListen = shouldListen,
                onRequestMicrophone = onRequestMicrophone,
                onListeningChange = onListeningChange,
                onSettings = { showSettings = true },
                onModeChange = onModeChange,
                onPresetChange = onPresetChange,
                onStringSelected = onStringSelected,
            )
        }
    }
}

@Composable
private fun MainTunerScreen(
    state: TunerViewModel.UiState,
    microphoneGranted: Boolean,
    shouldListen: Boolean,
    onRequestMicrophone: () -> Unit,
    onListeningChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onModeChange: (TunerViewModel.Mode) -> Unit,
    onPresetChange: (TunerViewModel.TuningPreset) -> Unit,
    onStringSelected: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PremiumHeader(
            listening = state.listening,
            shouldListen = shouldListen,
            onListeningChange = onListeningChange,
            onSettings = onSettings,
        )

        Spacer(Modifier.height(16.dp))

        if (!microphoneGranted) {
            MicrophonePermissionCard(onRequestMicrophone)
        } else {
            HeroTunerCard(state)

            if (state.mode == TunerViewModel.Mode.GUITAR) {
                Spacer(Modifier.height(14.dp))
                ManualStringCard(
                    state = state,
                    onStringSelected = onStringSelected,
                )

                Spacer(Modifier.height(12.dp))
                PresetSelector(
                    selected = state.preset,
                    onSelected = onPresetChange,
                )
            }

            Spacer(Modifier.height(12.dp))
            ModeSelector(
                selected = state.mode,
                onSelected = onModeChange,
            )

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = BlakeColors.Error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "FREE  •  OFFLINE  •  NO ADS  •  NO TRACKERS",
            color = BlakeColors.TextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}
