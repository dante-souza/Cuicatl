// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothScoRouteGateTest {
    @Test
    fun `disconnected before connected keeps waiting for handshake`() {
        val gate = BluetoothScoRouteGate(42)
        gate.observeScoState(false)
        assertFalse(gate.isConnected())
        assertFalse(gate.isLost())
        gate.observeScoState(true)
        assertTrue(gate.isConnected())
        assertNull(gate.captureFailure(42, true))
    }

    @Test
    fun `disconnect after connected is terminal even after reconnect broadcast`() {
        val gate = BluetoothScoRouteGate(42)
        gate.observeScoState(true)
        gate.observeScoState(false)
        gate.observeScoState(true)
        assertTrue(gate.isLost())
        assertFalse(gate.isConnected())
        assertTrue(gate.captureFailure(42, true)!!.contains("disconnected"))
    }

    @Test
    fun `capture cannot proceed before handshake`() {
        val gate = BluetoothScoRouteGate(42)
        assertTrue(gate.captureFailure(42, true)!!.contains("not confirmed"))
    }

    @Test
    fun `matching device id but wrong input type is rejected`() {
        val gate = BluetoothScoRouteGate(42)
        gate.observeScoState(true)
        assertFalse(gate.routeMatches(42, false))
        assertTrue(gate.captureFailure(42, false)!!.contains("route changed"))
    }

    @Test
    fun `missing route fails closed for explicitly selected SCO`() {
        val gate = BluetoothScoRouteGate(42)
        gate.observeScoState(true)
        assertTrue(gate.captureFailure(null, false)!!.contains("disappeared"))
    }

    @Test
    fun `alternate device cannot silently replace requested SCO device`() {
        val gate = BluetoothScoRouteGate(42)
        gate.observeScoState(true)
        assertEquals(
            "Bluetooth SCO capture route changed; expected device 42, actual device 77",
            gate.captureFailure(77, true),
        )
    }

    @Test
    fun `a new start requires a new gate after disconnect`() {
        val first = BluetoothScoRouteGate(42)
        first.observeScoState(true)
        first.observeScoState(false)
        val second = BluetoothScoRouteGate(42)
        second.observeScoState(true)
        assertTrue(first.isLost())
        assertNull(second.captureFailure(42, true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid device selection is rejected`() {
        BluetoothScoRouteGate(-1)
    }
}
