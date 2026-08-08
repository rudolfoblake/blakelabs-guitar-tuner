package com.blakelabs.guitartuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.TunerViewModel
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun HeroTunerCard(state: TunerViewModel.UiState) {
    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            TunerViewModel.PitchStatus.IN_TUNE -> BlakeColors.Primary
            TunerViewModel.PitchStatus.FLAT,
            TunerViewModel.PitchStatus.SHARP -> BlakeColors.Warning
            TunerViewModel.PitchStatus.WAITING -> BlakeColors.TextMuted
        },
        label = "statusColor",
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(26.dp)),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = if (state.isInTune) 0.115f else 0.045f),
                                Color.Transparent,
                            ),
                            radius = 640f,
                        ),
                    ),
            )

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (state.mode == TunerViewModel.Mode.GUITAR) {
                        "${state.preset.label.uppercase(Locale.US)} TUNING"
                    } else {
                        "CHROMATIC"
                    },
                    color = BlakeColors.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.45.sp,
                )

                Spacer(Modifier.height(3.dp))
                Text(
                    text = state.noteLabel,
                    color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Text,
                    fontSize = 88.sp,
                    lineHeight = 90.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2.6).sp,
                )

                Text(
                    text = state.frequencyHz?.let { "${formatHz(it)} Hz" } ?: "Play a string",
                    color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                )

                Spacer(Modifier.height(2.dp))
                TunerGauge(
                    cents = state.cents.toFloat(),
                    active = state.frequencyHz != null,
                    color = statusColor,
                )

                GaugeLabels()

                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (state.frequencyHz == null) "—" else String.format(Locale.US, "%+.1f", state.cents),
                    color = if (state.frequencyHz == null) BlakeColors.TextMuted else BlakeColors.Text,
                    fontSize = 24.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "CENTS",
                    color = BlakeColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )

                Spacer(Modifier.height(10.dp))
                StatusPill(state.status, statusColor)

                Spacer(Modifier.height(13.dp))
                SignalAndConfidence(state)
            }
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
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 390f),
        label = "needle",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .semantics {
                contentDescription = if (active) {
                    "Tuning gauge, ${animatedCents.roundToInt()} cents"
                } else {
                    "Tuning gauge, waiting for a note"
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height * 0.97f)
        val radius = minOf(size.width * 0.44f, size.height * 0.91f)
        val startAngle = 205f
        val sweep = 130f
        val arcSize = Size(radius * 2f, radius * 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)

        drawArc(
            color = BlakeColors.BorderStrong,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        val tuneZoneStart = startAngle + sweep * 0.47f
        val tuneZoneSweep = sweep * 0.06f
        drawArc(
            color = BlakeColors.Primary.copy(alpha = 0.24f),
            startAngle = tuneZoneStart,
            sweepAngle = tuneZoneSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = BlakeColors.Primary,
            startAngle = tuneZoneStart,
            sweepAngle = tuneZoneSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )

        for (value in -50..50 step 5) {
            val fraction = (value + 50f) / 100f
            val angle = startAngle + sweep * fraction
            val radians = angle * PI.toFloat() / 180f
            val major = value % 25 == 0
            val centerTick = value == 0
            val outer = radius
            val inner = radius * when {
                centerTick -> 0.78f
                major -> 0.84f
                else -> 0.91f
            }

            drawLine(
                color = when {
                    centerTick -> BlakeColors.Primary
                    major -> BlakeColors.TextMuted.copy(alpha = 0.7f)
                    else -> BlakeColors.BorderStrong
                },
                start = Offset(
                    center.x + cos(radians) * inner,
                    center.y + sin(radians) * inner,
                ),
                end = Offset(
                    center.x + cos(radians) * outer,
                    center.y + sin(radians) * outer,
                ),
                strokeWidth = when {
                    centerTick -> 2.5.dp.toPx()
                    major -> 1.7.dp.toPx()
                    else -> 1.dp.toPx()
                },
                cap = StrokeCap.Round,
            )
        }

        val needleFraction = (animatedCents + 50f) / 100f
        val needleAngle = startAngle + sweep * needleFraction
        val needleRadians = needleAngle * PI.toFloat() / 180f
        val needleEnd = Offset(
            center.x + cos(needleRadians) * radius * 0.79f,
            center.y + sin(needleRadians) * radius * 0.79f,
        )
        val needleColor = if (active) color else BlakeColors.TextMuted.copy(alpha = 0.28f)

        if (active) {
            drawLine(
                color = needleColor.copy(alpha = 0.13f),
                start = center,
                end = needleEnd,
                strokeWidth = 12.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = needleColor,
            start = center,
            end = needleEnd,
            strokeWidth = 3.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = BlakeColors.Surface,
            radius = 7.5.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = needleColor,
            radius = 5.dp.toPx(),
            center = center,
        )
    }
}

@Composable
private fun GaugeLabels() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("-50", "-25", "0", "+25", "+50").forEach { label ->
            Text(
                text = label,
                color = if (label == "0") BlakeColors.Primary else BlakeColors.TextDim,
                fontSize = 10.sp,
                fontWeight = if (label == "0") FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun StatusPill(
    status: TunerViewModel.PitchStatus,
    color: Color,
) {
    val label = when (status) {
        TunerViewModel.PitchStatus.WAITING -> "LISTENING"
        TunerViewModel.PitchStatus.FLAT -> "FLAT  •  TUNE UP"
        TunerViewModel.PitchStatus.IN_TUNE -> "✓  IN TUNE"
        TunerViewModel.PitchStatus.SHARP -> "SHARP  •  TUNE DOWN"
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.78f), RoundedCornerShape(999.dp)),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.25.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SignalAndConfidence(state: TunerViewModel.UiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricBar(
            label = "SIGNAL",
            value = state.signal,
            modifier = Modifier.weight(1f),
        )
        MetricBar(
            label = "LOCK",
            value = state.confidence,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = BlakeColors.TextDim,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = "${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                color = BlakeColors.TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BlakeColors.Border, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(BlakeColors.Primary, RoundedCornerShape(999.dp)),
            )
        }
    }
}

private fun formatHz(value: Double): String = when {
    value >= 1000.0 -> String.format(Locale.US, "%.1f", value)
    else -> String.format(Locale.US, "%.2f", value)
}
