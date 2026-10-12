// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.platform

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import io.github.dante_souza.cuicatl.domain.InputRouteKind

data class DiscoveredAudioInput(
    val id: Int,
    val type: Int,
    val name: String,
    val routeKind: InputRouteKind,
    val address: String,
)

class AndroidAudioInputDiscovery(context: Context) {

    private val audioManager =
        context.applicationContext.getSystemService(AudioManager::class.java)

    fun discover(): List<DiscoveredAudioInput> =
        audioManager
            ?.getDevices(AudioManager.GET_DEVICES_INPUTS)
            ?.filter { it.isSource }
            ?.map { device ->
                DiscoveredAudioInput(
                    id = device.id,
                    type = device.type,
                    name = device.productName.toString(),
                    routeKind = classify(device.type),
                    address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        device.address.orEmpty()
                    } else {
                        ""
                    },
                )
            }
            ?.sortedWith(compareBy({ it.routeKind.name }, { it.name }, { it.id }))
            ?: emptyList()

    private fun classify(type: Int): InputRouteKind =
        when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC ->
                InputRouteKind.BUILT_IN

            AudioDeviceInfo.TYPE_WIRED_HEADSET ->
                InputRouteKind.WIRED

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET ->
                InputRouteKind.USB

            AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                InputRouteKind.BLUETOOTH

            else ->
                InputRouteKind.OTHER
        }
}
