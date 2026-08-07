package com.blakelabs.guitartuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.TunerViewModel

@Composable
internal fun ManualStringCard(
    state: TunerViewModel.UiState,
    onStringSelected: (Int?) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(20.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "MANUAL STRING",
                    color = BlakeColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.25.sp,
                )
                SmallChip(
                    label = "AUTO",
                    selected = state.selectedStringIndex == null,
                    onClick = { onStringSelected(null) },
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.preset.strings.forEachIndexed { index, string ->
                    StringCircle(
                        label = string.label,
                        selected = state.selectedStringIndex == index,
                        onClick = { onStringSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StringCircle(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) BlakeColors.Primary else BlakeColors.SurfaceRaised,
        modifier = Modifier
            .size(50.dp)
            .border(
                width = 1.dp,
                color = if (selected) BlakeColors.Primary else BlakeColors.BorderStrong,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = label.dropLast(1),
                    color = if (selected) BlakeColors.Background else BlakeColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = label.takeLast(1),
                    color = if (selected) BlakeColors.Background.copy(alpha = 0.74f) else BlakeColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
internal fun PresetSelector(
    selected: TunerViewModel.TuningPreset,
    onSelected: (TunerViewModel.TuningPreset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TUNING PRESETS",
            color = BlakeColors.TextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TunerViewModel.TuningPreset.entries.forEach { preset ->
                SmallChip(
                    label = preset.label,
                    selected = preset == selected,
                    onClick = { onSelected(preset) },
                )
            }
        }
    }
}

@Composable
private fun SmallChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) BlakeColors.Primary.copy(alpha = 0.1f) else BlakeColors.Surface,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) BlakeColors.Primary else BlakeColors.Border,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) BlakeColors.Primary else BlakeColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
internal fun ModeSelector(
    selected: TunerViewModel.Mode,
    onSelected: (TunerViewModel.Mode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(18.dp))
            .padding(4.dp),
    ) {
        TunerViewModel.Mode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (active) BlakeColors.SurfaceRaised else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) BlakeColors.Primary.copy(alpha = 0.45f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelected(mode) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (mode == TunerViewModel.Mode.GUITAR) "GUITAR" else "CHROMATIC",
                    color = if (active) BlakeColors.Primary else BlakeColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
            }
        }
    }
}

@Composable
internal fun MicrophonePermissionCard(onRequestMicrophone: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(24.dp)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(size = 86.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "MICROPHONE NEEDED",
                color = BlakeColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.25.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "We need the mic to hear the string. The audio is analyzed locally and never leaves your phone.",
                color = BlakeColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRequestMicrophone,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlakeColors.Primary,
                    contentColor = BlakeColors.Background,
                ),
            ) {
                Text(
                    text = "ALLOW MICROPHONE",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                text = "No internet permission. Seriously.",
                color = BlakeColors.TextDim,
                fontSize = 10.sp,
            )
        }
    }
}
