package com.blakelabs.guitartuner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blakelabs.guitartuner.R

@Composable
fun BrandSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlakeColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BlakeColors.Primary.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        radius = 740f,
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            BrandMark(size = 172.dp)
            Spacer(Modifier.height(26.dp))
            Text(
                text = "BLAKE LABS",
                color = BlakeColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 5.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "GUITAR TUNER",
                color = BlakeColors.Primary,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            BrandWaveform()
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Free. Offline. No ads. No nonsense.",
                color = BlakeColors.TextMuted,
                fontSize = 14.sp,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun PremiumHeader(
    listening: Boolean,
    shouldListen: Boolean,
    onListeningChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "BLAKE LABS",
                    color = BlakeColors.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.1.sp,
                )
                Text(
                    text = "GUITAR TUNER",
                    color = BlakeColors.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.45.sp,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MicPill(
                listening = listening,
                shouldListen = shouldListen,
                onClick = { onListeningChange(!shouldListen) },
            )
            IconButton(onClick = onSettings)
        }
    }
}

@Composable
private fun MicPill(
    listening: Boolean,
    shouldListen: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = BlakeColors.Surface,
        modifier = Modifier
            .border(1.dp, BlakeColors.Border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (listening) BlakeColors.Primary else BlakeColors.TextDim,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (shouldListen) "MIC" else "OFF",
                color = if (shouldListen) BlakeColors.Text else BlakeColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
        }
    }
}

@Composable
private fun IconButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = BlakeColors.Surface,
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, BlakeColors.Border, CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(16.dp)) {
                val ys = listOf(size.height * 0.24f, size.height * 0.5f, size.height * 0.76f)
                ys.forEachIndexed { index, y ->
                    val start = if (index == 1) size.width * 0.1f else size.width * 0.24f
                    drawLine(
                        color = BlakeColors.Primary,
                        start = Offset(start, y),
                        end = Offset(size.width - start, y),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = BlakeColors.Primary,
                        radius = 2.dp.toPx(),
                        center = Offset(if (index == 1) size.width * 0.68f else size.width * 0.42f, y),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BrandMark(size: Dp) {
    Image(
        painter = painterResource(R.drawable.blake_labs_logo_official),
        contentDescription = "Blake Labs alien logo",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun BrandWaveform() {
    Canvas(
        modifier = Modifier
            .width(150.dp)
            .height(24.dp),
    ) {
        val amplitudes = listOf(0.12f, 0.32f, 0.18f, 0.62f, 0.28f, 0.82f, 0.4f, 1f, 0.4f, 0.82f, 0.28f, 0.62f, 0.18f, 0.32f, 0.12f)
        val spacing = size.width / (amplitudes.size + 1)
        amplitudes.forEachIndexed { index, amplitude ->
            val x = spacing * (index + 1)
            val half = size.height * 0.5f * amplitude
            drawLine(
                color = BlakeColors.Primary.copy(alpha = 0.9f),
                start = Offset(x, size.height / 2f - half),
                end = Offset(x, size.height / 2f + half),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
