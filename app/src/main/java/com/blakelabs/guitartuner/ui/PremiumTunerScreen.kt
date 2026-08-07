package com.blakelabs.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.R
import com.blakelabs.guitartuner.TunerViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun PremiumTunerScreen(
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

    LaunchedEffect(state.isInTune) {
        if (state.isInTune && !previouslyInTune) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previouslyInTune = state.isInTune
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlakeColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandHeader(
            listening = state.listening,
            shouldListen = shouldListen,
            onListeningChange = onListeningChange,
        )
        Spacer(Modifier.height(18.dp))
        PresetHeader(
            mode = state.mode,
            preset = state.preset,
            onPresetChange = onPresetChange,
        )
        Spacer(Modifier.height(8.dp))

        if (!microphoneGranted) {
            MicrophonePermissionCard(onRequestMicrophone)
        } else {
            TunerHero(state)
            Spacer(Modifier.height(14.dp))

            if (state.mode == TunerViewModel.Mode.GUITAR) {
                ManualStrings(state, onStringSelected)
                Spacer(Modifier.height(12.dp))
                TuningPresets(state.preset, onPresetChange)
            }

            Spacer(Modifier.height(12.dp))
            QuickSettings(
                mode = state.mode,
                a4Hz = state.a4Hz,
                onModeChange = onModeChange,
                onA4Change = onA4Change,
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

        Spacer(Modifier.height(18.dp))
        Text(
            text = "FREE  •  OFFLINE  •  NO ADS  •  NO NONSENSE",
            color = BlakeColors.TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.25.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun BrandHeader(
    listening: Boolean,
    shouldListen: Boolean,
    onListeningChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = BlakeColors.SurfaceRaised,
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, BlakeColors.Primary.copy(alpha = 0.45f), CircleShape),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_blake_alien),
                    contentDescription = "Blake Labs",
                    modifier = Modifier.padding(5.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "BLAKE LABS",
                    color = BlakeColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.2.sp,
                )
                Text(
                    text = "GUITAR TUNER",
                    color = BlakeColors.Primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (shouldListen) BlakeColors.SurfaceRaised else BlakeColors.Surface,
            modifier = Modifier
                .border(
                    1.dp,
                    if (shouldListen) BlakeColors.Primary.copy(alpha = 0.38f) else BlakeColors.Border,
                    RoundedCornerShape(999.dp),
                )
                .clickable { onListeningChange(!shouldListen) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (listening) BlakeColors.Primary else BlakeColors.TextMuted,
                            shape = CircleShape,
                        ),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (shouldListen) "LIVE" else "PAUSED",
                    color = if (shouldListen) BlakeColors.Text else BlakeColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
            }
        }
    }
}

@Composable
private fun PresetHeader(
    mode: TunerViewModel.Mode,
    preset: TunerViewModel.TuningPreset,
    onPresetChange: (TunerViewModel.TuningPreset) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (mode == TunerViewModel.Mode.GUITAR) preset.label.uppercase() else "CHROMATIC",
            color = BlakeColors.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            modifier = Modifier
                .clickable(enabled = mode == TunerViewModel.Mode.GUITAR) {
                    val presets = TunerViewModel.TuningPreset.entries
                    onPresetChange(presets[(preset.ordinal + 1) % presets.size])
                }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        if (mode == TunerViewModel.Mode.GUITAR) {
            Text("⌄", color = BlakeColors.Primary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TunerHero(state: TunerViewModel.UiState) {
    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            TunerViewModel.PitchStatus.IN_TUNE -> BlakeColors.Primary
            TunerViewModel.PitchStatus.FLAT,
            TunerViewModel.PitchStatus.SHARP -> BlakeColors.Warning
            TunerViewModel.PitchStatus.WAITING -> BlakeColors.TextMuted
        },
        label = "statusColor",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(24.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.noteLabel,
            color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Text,
            fontSize = 76.sp,
            lineHeight = 78.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
        )
        Text(
            text = state.frequencyHz?.let { "${formatHz(it)} Hz" } ?: "Play a string",
            color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        TunerGauge(
            cents = state.cents.toFloat(),
            active = state.frequencyHz != null,
            color = statusColor,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("-50", color = BlakeColors.TextMuted, fontSize = 11.sp)
            Text("-25", color = BlakeColors.TextMuted, fontSize = 11.sp)
            Text("0", color = BlakeColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("+25", color = BlakeColors.TextMuted, fontSize = 11.sp)
            Text("+50", color = BlakeColors.TextMuted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.frequencyHz == null) "—" else String.format(Locale.US, "%+.1f", state.cents),
            color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "CENTS",
            color = BlakeColors.TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(12.dp))
        StatusPill(state.status, statusColor)
    }
}

@Composable
private fun StatusPill(status: TunerViewModel.PitchStatus, color: Color) {
    val text = when (status) {
        TunerViewModel.PitchStatus.WAITING -> "LISTENING"
        TunerViewModel.PitchStatus.FLAT -> "FLAT  •  TUNE UP"
        TunerViewModel.PitchStatus.IN_TUNE -> "✓  IN TUNE"
        TunerViewModel.PitchStatus.SHARP -> "SHARP  •  TUNE DOWN"
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.10f),
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .border(1.dp, color.copy(alpha = 0.78f), RoundedCornerShape(999.dp)),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun TunerGauge(cents: Float, active: Boolean, color: Color) {
    val animatedCents by animateFloatAsState(
        targetValue = cents.coerceIn(-50f, 50f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 440f),
        label = "needle",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .semantics {
                contentDescription = if (active) {
                    "Tuning gauge, ${animatedCents.roundToInt()} cents"
                } else {
                    "Tuning gauge, waiting for a note"
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height * 0.94f)
        val radius = minOf(size.width * 0.44f, size.height * 0.91f)
        val startAngle = 205f
        val sweep = 130f

        drawArc(
            color = BlakeColors.Border,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = BlakeColors.Primary.copy(alpha = if (active) 0.72f else 0.18f),
            startAngle = 263f,
            sweepAngle = 14f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        for (value in -50..50 step 5) {
            val fraction = (value + 50f) / 100f
            val angle = startAngle + (sweep * fraction)
            val radians = angle * PI.toFloat() / 180f
            val major = value % 10 == 0
            val outer = radius
            val inner = radius * if (major) 0.84f else 0.91f
            val tickColor = if (value in -5..5) {
                BlakeColors.Primary.copy(alpha = if (active) 0.86f else 0.25f)
            } else {
                BlakeColors.TextMuted.copy(alpha = 0.55f)
            }
            drawLine(
                color = tickColor,
                start = Offset(
                    center.x + cos(radians) * inner,
                    center.y + sin(radians) * inner,
                ),
                end = Offset(
                    center.x + cos(radians) * outer,
                    center.y + sin(radians) * outer,
                ),
                strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        val needleFraction = (animatedCents + 50f) / 100f
        val needleAngle = startAngle + (sweep * needleFraction)
        val needleRadians = needleAngle * PI.toFloat() / 180f
        val needleEnd = Offset(
            center.x + cos(needleRadians) * radius * 0.78f,
            center.y + sin(needleRadians) * radius * 0.78f,
        )
        drawLine(
            color = if (active) color else BlakeColors.TextMuted.copy(alpha = 0.28f),
            start = center,
            end = needleEnd,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = if (active) color else BlakeColors.TextMuted.copy(alpha = 0.28f),
            radius = 5.dp.toPx(),
            center = center,
        )
    }
}

@Composable
private fun ManualStrings(
    state: TunerViewModel.UiState,
    onStringSelected: (Int?) -> Unit,
) {
    SectionCard("MANUAL STRING") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            StringChip("AUTO", state.selectedStringIndex == null) { onStringSelected(null) }
            state.preset.strings.forEachIndexed { index, string ->
                StringChip(string.label, state.selectedStringIndex == index) { onStringSelected(index) }
            }
        }
    }
}

@Composable
private fun TuningPresets(
    selected: TunerViewModel.TuningPreset,
    onPresetChange: (TunerViewModel.TuningPreset) -> Unit,
) {
    SectionCard("TUNING PRESETS") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TunerViewModel.TuningPreset.entries.forEach { preset ->
                PresetChip(preset.label, preset == selected) { onPresetChange(preset) }
            }
        }
    }
}

@Composable
private fun QuickSettings(
    mode: TunerViewModel.Mode,
    a4Hz: Double,
    onModeChange: (TunerViewModel.Mode) -> Unit,
    onA4Change: (Double) -> Unit,
) {
    SectionCard("QUICK SETTINGS") {
        Text(
            text = "MODE",
            color = BlakeColors.TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeChip(
                label = "GUITAR",
                selected = mode == TunerViewModel.Mode.GUITAR,
                modifier = Modifier.weight(1f),
            ) { onModeChange(TunerViewModel.Mode.GUITAR) }
            ModeChip(
                label = "CHROMATIC",
                selected = mode == TunerViewModel.Mode.CHROMATIC,
                modifier = Modifier.weight(1f),
            ) { onModeChange(TunerViewModel.Mode.CHROMATIC) }
        }

        Spacer(Modifier.height(15.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "A4 REFERENCE",
                    color = BlakeColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = String.format(Locale.US, "%.1f Hz", a4Hz),
                    color = BlakeColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundActionButton("−") { onA4Change(-0.5) }
                RoundActionButton("+") { onA4Change(0.5) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Offline. No ads. No trackers. Your microphone audio stays on this device.",
            color = BlakeColors.TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(
            text = title,
            color = BlakeColors.TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun StringChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) BlakeColors.Primary else BlakeColors.SurfaceSoft,
        modifier = Modifier
            .size(if (label == "AUTO") 52.dp else 44.dp)
            .border(
                width = 1.dp,
                color = if (selected) BlakeColors.Primary else BlakeColors.Border,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) BlakeColors.Background else BlakeColors.Text,
                fontSize = if (label == "AUTO") 9.sp else 13.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) BlakeColors.Primary.copy(alpha = 0.12f) else BlakeColors.SurfaceSoft,
        modifier = Modifier
            .border(
                1.dp,
                if (selected) BlakeColors.Primary else BlakeColors.Border,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) BlakeColors.Primary else BlakeColors.Text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BlakeColors.Primary.copy(alpha = 0.10f) else BlakeColors.SurfaceSoft,
        modifier = modifier
            .border(
                1.dp,
                if (selected) BlakeColors.Primary else BlakeColors.Border,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) BlakeColors.Primary else BlakeColors.TextMuted,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(vertical = 13.dp),
        )
    }
}

@Composable
private fun RoundActionButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = BlakeColors.SurfaceSoft,
        modifier = Modifier
            .size(40.dp)
            .border(1.dp, BlakeColors.Border, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = BlakeColors.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

@Composable
private fun MicrophonePermissionCard(onRequestMicrophone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, BlakeColors.Primary.copy(alpha = 0.32f), RoundedCornerShape(22.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_blake_alien),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "LET ME HEAR THE GUITAR",
            color = BlakeColors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Microphone access is only used for on-device pitch detection. Nothing is recorded or uploaded.",
            color = BlakeColors.TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestMicrophone,
            colors = ButtonDefaults.buttonColors(
                containerColor = BlakeColors.Primary,
                contentColor = BlakeColors.Background,
            ),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = "ENABLE MICROPHONE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private fun formatHz(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.US, "%.0f", value)
    value >= 100.0 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
