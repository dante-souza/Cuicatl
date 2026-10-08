// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.dante_souza.cuicatl.ui.CuicatlApp
import io.github.dante_souza.cuicatl.ui.CuicatlBrandSplash
import io.github.dante_souza.cuicatl.ui.theme.CuicatlTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val showBrandSplashOnLaunch = savedInstanceState == null
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (showBrandSplashOnLaunch) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            CuicatlTheme {
                var showBrandSplash by remember {
                    mutableStateOf(showBrandSplashOnLaunch)
                }

                LaunchedEffect(showBrandSplash) {
                    if (showBrandSplash) {
                        delay(BRAND_SPLASH_DURATION_MS)
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                        WindowCompat.setDecorFitsSystemWindows(window, true)
                        showBrandSplash = false
                    }
                }

                if (showBrandSplash) {
                    CuicatlBrandSplash()
                } else {
                    CuicatlApp()
                }
            }
        }
    }

    private companion object {
        const val BRAND_SPLASH_DURATION_MS = 900L
    }
}
