package com.blakelabs.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.TunerViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

    LaunchedEffect(state.isInTune) {
        if (state.isInTune && !previouslyInTune) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previouslyInTune = state.isInTune
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header(
            listening = state.listening,
            shouldListen = shouldListen,
            onListeningChange = onListeningChange,
        )

        Spacer(Modifier.height(18.dp))

        ModeSelector(
            selected = state.mode,
            onSelected = onModeChange,
        )

        Spacer(Modifier.height(22.dp))

        if (!microphoneGranted) {
            MicrophonePermissionCard(onRequestMicrophone)
            Spacer(Modifier.weight(1f))
        } else {
            TunerReadout(state)

            Spacer(Modifier.height(16.dp))

            if (state.mode == TunerViewModel.Mode.GUITAR) {
                GuitarControls(
                    state = state,
                    onPresetChange = onPresetChange,
                    onStringSelected = onStringSelected,
                )
                Spacer(Modifier.height(14.dp))
            }

            CalibrationControl(
                a4Hz = state.a4Hz,
                onChange = onA4Change,
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

            Spacer(Modifier.weight(1f))
        }

        Text(
            text = "NO ADS  •  NO TRACKERS  •  NO NONSENSE",
            color = BlakeColors.TextMuted,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun Header(
    listening: Boolean,
    shouldListen: Boolean,
    onListeningChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "BLAKE LABS",
                color = BlakeColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp,
            )
            Text(
                text = "GUITAR TUNER",
                color = BlakeColors.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (shouldListen) BlakeColors.SurfaceRaised else BlakeColors.Surface,
            modifier = Modifier
                .border(1.dp, BlakeColors.Border, RoundedCornerShape(999.dp))
                .clickable { onListeningChange(!shouldListen) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                    text = if (shouldListen) "MIC ON" else "MIC OFF",
                    color = BlakeColors.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    selected: TunerViewModel.Mode,
    onSelected: (TunerViewModel.Mode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        TunerViewModel.Mode.entries.forEach { mode ->
            val active = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (active) BlakeColors.SurfaceRaised else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelected(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (mode == TunerViewModel.Mode.GUITAR) "GUITAR" else "CHROMATIC",
                    color = if (active) BlakeColors.Primary else BlakeColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun TunerReadout(state: TunerViewModel.UiState) {
    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            TunerViewModel.PitchStatus.IN_TUNE -> BlakeColors.Primary
            TunerViewModel.PitchStatus.FLAT,
            TunerViewModel.PitchStatus.SHARP -> BlakeColors.Warning
            TunerViewModel.PitchStatus.WAITING -> BlakeColors.TextMuted
        },
        label = "statusColor",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.noteLabel,
            color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Text,
            fontSize = 78.sp,
            lineHeight = 80.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-2).sp,
        )

        Text(
            text = state.frequencyHz?.let { "${formatHz(it)} Hz" } ?: "Play a string",
            color = BlakeColors.TextMuted,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(12.dp))

        TunerGauge(
            cents = state.cents.toFloat(),
            active = state.frequencyHz != null,
            color = statusColor,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("-50", color = BlakeColors.TextMuted, fontSize = 11.sp)
            Text("0", color = BlakeColors.TextMuted, fontSize = 11.sp)
            Text("+50", color = BlakeColors.TextMuted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(12.dp))

        val statusText = when (state.status) {
            TunerViewModel.PitchStatus.WAITING -> "LISTENING"
            TunerViewModel.PitchStatus.FLAT -> "FLAT  •  TUNE UP"
            TunerViewModel.PitchStatus.IN_TUNE -> "IN TUNE"
            TunerViewModel.PitchStatus.SHARP -> "SHARP  •  TUNE DOWN"
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.border(1.dp, statusColor.copy(alpha = 0.65f), RoundedCornerShape(999.dp)),
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            )
        }

        if (state.frequencyHz != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.US, "%+.1f cents", state.cents),
                color = BlakeColors.TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TunerGauge(
    cents: Float,
    active: Boolean,
    color: Color,
) {
    val animatedCents by animateFloatAsState(
        targetValue = cents.coerceIn(-50f, 50f),
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
        label = "needle",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics {
                contentDescription = if (active) {
                    "Tuning gauge, ${animatedCents.roundToInt()} cents"
                } else {
                    "Tuning gauge, waiting for a note"
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height * 0.95f)
        val radius = minOf(size.width * 0.43f, size.height * 0.88f)
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

        for (value in -50..50 step 5) {
            val fraction = (value + 50f) / 100f
            val angle = startAngle + (sweep * fraction)
            val radians = angle * PI.toFloat() / 180f
            val major = value % 10 == 0
            val outer = radius
            val inner = radius * if (major) 0.86f else 0.91f
            val tickColor = if (value == 0) BlakeColors.PrimaryMuted else BlakeColors.Border

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
            center.x + cos(needleRadians) * radius * 0.82f,
            center.y + sin(needleRadians) * radius * 0.82f,
        )

        drawLine(
            color = if (active) color else BlakeColors.TextMuted.copy(alpha = 0.35f),
            start = center,
            end = needleEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = if (active) color else BlakeColors.TextMuted.copy(alpha = 0.35f),
            radius = 5.dp.toPx(),
            center = center,
        )
    }
}

@Composable
private fun GuitarControls(
    state: TunerViewModel.UiState,
    onPresetChange: (TunerViewModel.TuningPreset) -> Unit,
    onStringSelected: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "TUNING",
                    color = BlakeColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = state.preset.label,
                    color = BlakeColors.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = "CHANGE",
                color = BlakeColors.Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        val entries = TunerViewModel.TuningPreset.entries
                        val next = entries[(state.preset.ordinal + 1) % entries.size]
                        onPresetChange(next)
                    }
                    .padding(8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StringChip(
                label = "AUTO",
                selected = state.selectedStringIndex == null,
                onClick = { onStringSelected(null) },
            )
            state.preset.strings.forEachIndexed { index, string ->
                StringChip(
                    label = string.label,
                    selected = state.selectedStringIndex == index,
                    onClick = { onStringSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun StringChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) BlakeColors.Primary.copy(alpha = 0.12f) else BlakeColors.SurfaceRaised,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) BlakeColors.Primary.copy(alpha = 0.75f) else BlakeColors.Border,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) BlakeColors.Primary else BlakeColors.TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun CalibrationControl(
    a4Hz: Double,
    onChange: (Double) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "REFERENCE",
                color = BlakeColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "A4  ${a4Hz.roundToInt()} Hz",
                color = BlakeColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniButton("−") { onChange(-1.0) }
            MiniButton("+") { onChange(1.0) }
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = BlakeColors.SurfaceRaised,
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = BlakeColors.Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MicrophonePermissionCard(onRequestMicrophone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlakeColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "MICROPHONE NEEDED",
            color = BlakeColors.Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "The tuner analyzes the microphone locally. Audio is never uploaded, stored, or sent anywhere.",
            color = BlakeColors.TextMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequestMicrophone,
            colors = ButtonDefaults.buttonColors(
                containerColor = BlakeColors.Primary,
                contentColor = BlakeColors.Background,
            ),
        ) {
            Text("ALLOW MICROPHONE", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatHz(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
