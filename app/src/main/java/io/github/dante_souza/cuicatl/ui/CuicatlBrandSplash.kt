// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.dante_souza.cuicatl.R

@Composable
fun CuicatlBrandSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF161616)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.cuicatl_theme_d_splash),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
    }
}
