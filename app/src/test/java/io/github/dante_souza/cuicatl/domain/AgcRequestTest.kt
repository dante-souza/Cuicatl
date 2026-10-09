// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgcRequestTest {
    @Test
    fun defaultLeavesAndroidAgcStateUnchanged() {
        assertNull(AgcRequest.DEFAULT.targetEnabled())
    }

    @Test
    fun forceOffRequestsDisabledState() {
        assertEquals(false, AgcRequest.FORCE_OFF.targetEnabled())
    }

    @Test
    fun forceOnRequestsEnabledState() {
        assertEquals(true, AgcRequest.FORCE_ON.targetEnabled())
    }
}
