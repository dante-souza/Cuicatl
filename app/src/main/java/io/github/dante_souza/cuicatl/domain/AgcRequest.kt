// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

enum class AgcRequest {
    DEFAULT,
    FORCE_OFF,
    FORCE_ON;

    fun targetEnabled(): Boolean? = when (this) {
        DEFAULT -> null
        FORCE_OFF -> false
        FORCE_ON -> true
    }
}
