// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CuicatlColors = darkColorScheme(
    primary = Color(0xFFFFB45B),
    onPrimary = Color(0xFF321C00),
    secondary = Color(0xFFFFD8A8),
    background = Color(0xFF090C0F),
    surface = Color(0xFF11161B),
    onBackground = Color(0xFFE7EDF2),
    onSurface = Color(0xFFE7EDF2),
    outline = Color(0xFF89939C),
)

@Composable
fun CuicatlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CuicatlColors,
        content = content,
    )
}
