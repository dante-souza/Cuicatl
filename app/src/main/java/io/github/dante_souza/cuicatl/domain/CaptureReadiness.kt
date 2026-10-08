// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

sealed interface CaptureReadiness {
    data object Ready : CaptureReadiness

    data class NotReady(
        val reason: Reason,
        val detail: String? = null,
    ) : CaptureReadiness

    enum class Reason {
        MICROPHONE_PERMISSION_REQUIRED,
        MICROPHONE_UNAVAILABLE,
        INPUT_CONFIGURATION_UNSUPPORTED,
        PLATFORM_RESTRICTION,
        UNKNOWN,
    }
}
