// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.dante_souza.cuicatl.R
import kotlin.math.min
import kotlin.math.roundToInt

private val SplashGraphite = Color(0xFF161616)
private val SplashPink = Color(0xFFFF3FA4)
private val SplashPinkDeep = Color(0xFF5E2941)
private val SplashGold = Color(0xFFFFB45B)
private val SplashGoldDeep = Color(0xFF70582E)

@Composable
fun CuicatlBrandSplash() {
    val context = LocalContext.current
    val approvedIdentity = remember {
        BitmapFactory.decodeResource(
            context.resources,
            R.drawable.cuicatl_splash_identity,
        ).asImageBitmap()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = SplashGraphite)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashPinkDeep.copy(alpha = 0.24f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.72f, size.height * 0.30f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.72f, size.height * 0.30f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashGoldDeep.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.12f, size.height * 0.78f),
                radius = size.minDimension * 0.62f,
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.12f, size.height * 0.78f),
        )

        val upperArc = Path().apply {
            moveTo(-size.width * 0.10f, size.height * 0.10f)
            cubicTo(
                size.width * 0.18f,
                size.height * 0.01f,
                size.width * 0.72f,
                size.height * 0.08f,
                size.width * 1.08f,
                size.height * 0.30f,
            )
        }

        val lowerArc = Path().apply {
            moveTo(-size.width * 0.10f, size.height * 0.72f)
            cubicTo(
                size.width * 0.22f,
                size.height * 0.91f,
                size.width * 0.70f,
                size.height * 0.91f,
                size.width * 1.10f,
                size.height * 0.78f,
            )
        }

        val aurora = Brush.linearGradient(
            colors = listOf(
                SplashGold.copy(alpha = 0.70f),
                SplashPink.copy(alpha = 0.82f),
                SplashGold.copy(alpha = 0.58f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )

        listOf(upperArc, lowerArc).forEach { path ->
            drawPath(
                path = path,
                brush = aurora,
                style = Stroke(
                    width = 12.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.05f,
            )
            drawPath(
                path = path,
                brush = aurora,
                style = Stroke(
                    width = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = 0.78f,
            )
        }

        val imageWidth = approvedIdentity.width.toFloat()
        val imageHeight = approvedIdentity.height.toFloat()
        val fitScale = min(
            size.width / imageWidth,
            size.height / imageHeight,
        ) * 0.94f

        val dstWidth = (imageWidth * fitScale).roundToInt()
        val dstHeight = (imageHeight * fitScale).roundToInt()
        val dstX = ((size.width - dstWidth) / 2f).roundToInt()
        val dstY = ((size.height - dstHeight) / 2f).roundToInt()

        drawImage(
            image = approvedIdentity,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(
                approvedIdentity.width,
                approvedIdentity.height,
            ),
            dstOffset = IntOffset(dstX, dstY),
            dstSize = IntSize(dstWidth, dstHeight),
            alpha = 1f,
            blendMode = BlendMode.Screen,
        )
    }
}
