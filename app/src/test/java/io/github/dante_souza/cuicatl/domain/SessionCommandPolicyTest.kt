// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCommandPolicyTest {
    @Test
    fun startIsAcceptedOnlyWhenNoSessionIsOpeningRunningOrFinalizing() {
        assertTrue(SessionCommandPolicy.acceptsStart(SessionRuntimeSnapshot.Status.READY))
        assertTrue(SessionCommandPolicy.acceptsStart(SessionRuntimeSnapshot.Status.FAILED))

        assertFalse(SessionCommandPolicy.acceptsStart(SessionRuntimeSnapshot.Status.STARTING))
        assertFalse(SessionCommandPolicy.acceptsStart(SessionRuntimeSnapshot.Status.RUNNING))
        assertFalse(SessionCommandPolicy.acceptsStart(SessionRuntimeSnapshot.Status.FINALIZING))
    }

    @Test
    fun stopRequiresAnActiveCapture() {
        assertFalse(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.READY,
                hasActiveCapture = false,
            ),
        )
        assertFalse(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.FAILED,
                hasActiveCapture = false,
            ),
        )

        assertTrue(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.STARTING,
                hasActiveCapture = true,
            ),
        )
        assertTrue(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.RUNNING,
                hasActiveCapture = true,
            ),
        )
        assertTrue(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.FINALIZING,
                hasActiveCapture = true,
            ),
        )
    }

    @Test
    fun repeatedStopAfterCaptureIsGoneIsARejectedNoOp() {
        assertFalse(
            SessionCommandPolicy.acceptsStop(
                SessionRuntimeSnapshot.Status.READY,
                hasActiveCapture = false,
            ),
        )
    }
}
