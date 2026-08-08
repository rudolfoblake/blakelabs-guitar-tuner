package com.blakelabs.guitartuner.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.TunerViewModel
import java.util.Locale

@Composable
internal fun SettingsScreen(
    state: TunerViewModel.UiState,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onModeChange: (TunerViewModel.Mode) -> Unit,
    onA4Change: (Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = CircleShape,
                color = BlakeColors.Surface,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, BlakeColors.Border, CircleShape)
                    .clickable(onClick = onBack),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        color = BlakeColors.Text,
                        fontSize = 30.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            BrandMark(size = 40.dp)

            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "SET",
                    color = BlakeColors.Primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "SETTINGS",
            color = BlakeColors.Text,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.25.sp,
        )
        Text(
            text = "Tune the tuner. Very meta.",
            color = BlakeColors.TextMuted,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(18.dp))
        CalibrationCard(
            a4Hz = state.a4Hz,
            onA4Change = onA4Change,
        )

        Spacer(Modifier.height(12.dp))
        SettingsModeCard(
            selected = state.mode,
            onModeChange = onModeChange,
        )

        Spacer(Modifier.height(12.dp))
        HapticsCard(
            enabled = hapticsEnabled,
            onChange = onHapticsChange,
        )

        Spacer(Modifier.height(12.dp))
        PrivacyCard()

        Spacer(Modifier.height(22.dp))
        Text(
            text = "BLAKE LABS  //  GUITAR TUNER",
            color = BlakeColors.TextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun CalibrationCard(
    a4Hz: Double,
    onA4Change: (Double) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(a4Hz.toFloat()) }

    LaunchedEffect(a4Hz) {
        sliderValue = a4Hz.toFloat()
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "CALIBRATION",
                color = BlakeColors.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A4 REFERENCE",
                color = BlakeColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", a4Hz),
                    color = BlakeColors.Text,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Hz",
                    color = BlakeColors.TextMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { requested ->
                    val delta = requested - sliderValue
                    sliderValue = requested
                    onA4Change(delta.toDouble())
                },
                valueRange = 430f..450f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = BlakeColors.Primary,
                    activeTrackColor = BlakeColors.Primary,
                    inactiveTrackColor = BlakeColors.BorderStrong,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("430", color = BlakeColors.TextDim, fontSize = 10.sp)
                Text("440", color = BlakeColors.Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("450", color = BlakeColors.TextDim, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SettingsModeCard(
    selected: TunerViewModel.Mode,
    onModeChange: (TunerViewModel.Mode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MODE",
                color = BlakeColors.TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TunerViewModel.Mode.entries.forEach { mode ->
                    val active = mode == selected
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (active) BlakeColors.Primary.copy(alpha = 0.08f) else BlakeColors.SurfaceRaised,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = if (active) BlakeColors.Primary else BlakeColors.BorderStrong,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onModeChange(mode) },
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (mode == TunerViewModel.Mode.GUITAR) "♬" else "≋",
                                color = if (active) BlakeColors.Primary else BlakeColors.TextMuted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (mode == TunerViewModel.Mode.GUITAR) "Guitar" else "Chromatic",
                                color = if (active) BlakeColors.Primary else BlakeColors.Text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HapticsCard(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HAPTIC FEEDBACK",
                    color = BlakeColors.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Vibrate once when the note locks in tune",
                    color = BlakeColors.TextMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BlakeColors.Background,
                    checkedTrackColor = BlakeColors.Primary,
                    uncheckedThumbColor = BlakeColors.TextMuted,
                    uncheckedTrackColor = BlakeColors.BorderStrong,
                    uncheckedBorderColor = BlakeColors.BorderStrong,
                ),
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "PRIVACY FIRST",
                    color = BlakeColors.Primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Offline. No ads. No trackers. Audio stays on your phone.",
                    color = BlakeColors.TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
