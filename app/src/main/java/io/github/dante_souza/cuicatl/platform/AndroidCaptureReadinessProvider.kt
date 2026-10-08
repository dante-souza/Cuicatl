// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.github.dante_souza.cuicatl.domain.CaptureReadiness

class AndroidCaptureReadinessProvider(
    private val context: Context,
) {
    fun readiness(): CaptureReadiness {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            return CaptureReadiness.NotReady(
                reason = CaptureReadiness.Reason.MICROPHONE_UNAVAILABLE,
                detail = "Android reports no microphone feature.",
            )
        }

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return CaptureReadiness.NotReady(
                reason = CaptureReadiness.Reason.MICROPHONE_PERMISSION_REQUIRED,
                detail = "Microphone permission is requested only after Start probe.",
            )
        }

        return CaptureReadiness.Ready
    }
}
