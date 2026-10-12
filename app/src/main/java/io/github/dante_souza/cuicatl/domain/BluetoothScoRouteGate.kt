// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

/**
 * Fail-closed policy for a single explicitly selected Bluetooth SCO capture.
 *
 * Android broadcasts and the capture worker may run on different threads.
 * A connection lost after CONNECTED is terminal for this capture attempt;
 * reconnection requires another explicit Start and a new policy instance.
 * This policy does not imply physical microphone isolation or SPL calibration.
 */
class BluetoothScoRouteGate(private val requestedDeviceId: Int) {
    init {
        require(requestedDeviceId >= 0) { "Bluetooth input device ID is required" }
    }

    enum class Connection { WAITING, CONNECTED, LOST }

    @Volatile
    private var connection = Connection.WAITING

    @Synchronized
    fun observeScoState(connected: Boolean) {
        when {
            connection == Connection.LOST -> Unit
            connected -> connection = Connection.CONNECTED
            connection == Connection.CONNECTED -> connection = Connection.LOST
            // DISCONNECTED before the first CONNECTED is not a completed handshake.
        }
    }

    fun isConnected(): Boolean = connection == Connection.CONNECTED

    fun isLost(): Boolean = connection == Connection.LOST

    fun routeMatches(deviceId: Int?, isBluetoothSco: Boolean): Boolean =
        deviceId == requestedDeviceId && isBluetoothSco

    /**
     * Called during capture, including immediately before consuming a read.
     * Missing routing information is not acceptable for explicitly requested SCO.
     */
    fun captureFailure(deviceId: Int?, isBluetoothSco: Boolean): String? =
        when {
            connection == Connection.LOST ->
                "Bluetooth SCO disconnected during capture; session interrupted"
            connection != Connection.CONNECTED ->
                "Bluetooth SCO connection not confirmed during capture"
            deviceId == null ->
                "Bluetooth SCO capture route disappeared; session interrupted"
            !routeMatches(deviceId, isBluetoothSco) ->
                "Bluetooth SCO capture route changed; expected device $requestedDeviceId, actual device $deviceId"
            else -> null
        }
}
