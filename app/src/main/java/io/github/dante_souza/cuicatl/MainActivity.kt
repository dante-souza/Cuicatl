// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.dante_souza.cuicatl.ui.CuicatlApp
import io.github.dante_souza.cuicatl.ui.theme.CuicatlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            CuicatlTheme {
                CuicatlApp()
            }
        }
    }
}
