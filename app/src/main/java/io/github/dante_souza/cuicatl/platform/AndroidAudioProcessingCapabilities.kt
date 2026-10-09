// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.media.audiofx.AutomaticGainControl

object AndroidAudioProcessingCapabilities {
    fun isAgcAvailable(): Boolean = AutomaticGainControl.isAvailable()
}
