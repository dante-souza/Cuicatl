// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

object SessionCommandPolicy {
    fun acceptsStart(status: SessionRuntimeSnapshot.Status): Boolean =
        when (status) {
            SessionRuntimeSnapshot.Status.READY,
            SessionRuntimeSnapshot.Status.FAILED,
            -> true

            SessionRuntimeSnapshot.Status.STARTING,
            SessionRuntimeSnapshot.Status.RUNNING,
            SessionRuntimeSnapshot.Status.FINALIZING,
            -> false
        }

    fun acceptsStop(
        status: SessionRuntimeSnapshot.Status,
        hasActiveCapture: Boolean,
    ): Boolean =
        hasActiveCapture &&
            (
                status == SessionRuntimeSnapshot.Status.STARTING ||
                    status == SessionRuntimeSnapshot.Status.RUNNING ||
                    status == SessionRuntimeSnapshot.Status.FINALIZING
                )
}
