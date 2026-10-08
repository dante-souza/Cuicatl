// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dante_souza.cuicatl.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val SplashGraphite = Color(0xFF161616)
private val SplashPink = Color(0xFFFF3FA4)
private val SplashPinkDeep = Color(0xFF5E2941)
private val SplashGold = Color(0xFFFFB45B)
private val SplashGoldDeep = Color(0xFF70582E)
private val SplashText = Color(0xFFE9E0EA)

@Composable
fun CuicatlBrandSplash() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ThemeDAurora()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.cuicatl_app_icon),
                contentDescription = null,
                modifier = Modifier.size(232.dp),
            )

            Text(
                text = stringResource(R.string.app_name),
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SplashPink,
                            Color(0xFFFF5E8D),
                            SplashGold,
                        ),
                    ),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.sound_analysis_platform),
                color = SplashText,
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 3.2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ThemeDAurora() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = SplashGraphite)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashPinkDeep.copy(alpha = 0.38f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.67f, size.height * 0.34f),
                radius = size.minDimension * 0.78f,
            ),
            radius = size.minDimension * 0.78f,
            center = Offset(size.width * 0.67f, size.height * 0.34f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashGoldDeep.copy(alpha = 0.24f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.18f, size.height * 0.76f),
                radius = size.minDimension * 0.68f,
            ),
            radius = size.minDimension * 0.68f,
            center = Offset(size.width * 0.18f, size.height * 0.76f),
        )

        val upperArc = Path().apply {
            moveTo(-size.width * 0.08f, size.height * 0.12f)
            cubicTo(
                size.width * 0.18f,
                size.height * 0.02f,
                size.width * 0.72f,
                size.height * 0.08f,
                size.width * 1.08f,
                size.height * 0.31f,
            )
        }
        val lowerArc = Path().apply {
            moveTo(-size.width * 0.12f, size.height * 0.72f)
            cubicTo(
                size.width * 0.22f,
                size.height * 0.92f,
                size.width * 0.68f,
                size.height * 0.90f,
                size.width * 1.10f,
                size.height * 0.77f,
            )
        }

        val auroraBrush = Brush.linearGradient(
            colors = listOf(
                SplashGold.copy(alpha = 0.72f),
                SplashPink.copy(alpha = 0.82f),
                SplashGold.copy(alpha = 0.58f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )

        listOf(upperArc, lowerArc).forEach { path ->
            drawPath(
                path = path,
                brush = auroraBrush,
                style = Stroke(
                    width = 42.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.08f,
            )
            drawPath(
                path = path,
                brush = auroraBrush,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.72f,
            )
        }

        val centerY = size.height * 0.47f
        val count = 52
        val left = size.width * 0.04f
        val right = size.width * 0.96f
        val step = (right - left) / (count - 1)

        repeat(count) { index ->
            val x = left + step * index
            val normalized = (index - (count - 1) / 2f) / (count / 2f)
            val envelope = (1f - abs(normalized)).coerceAtLeast(0.08f)
            val harmonic = 0.32f + 0.68f * abs(sin(index * 0.73 * PI)).toFloat()
            val halfHeight = size.height * (0.012f + 0.062f * envelope * harmonic)

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashPink.copy(alpha = 0.10f),
                        SplashPink.copy(alpha = 0.72f),
                        SplashGold.copy(alpha = 0.70f),
                        SplashPink.copy(alpha = 0.10f),
                    ),
                    startY = centerY - halfHeight,
                    endY = centerY + halfHeight,
                ),
                start = Offset(x, centerY - halfHeight),
                end = Offset(x, centerY + halfHeight),
                strokeWidth = 1.35.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
