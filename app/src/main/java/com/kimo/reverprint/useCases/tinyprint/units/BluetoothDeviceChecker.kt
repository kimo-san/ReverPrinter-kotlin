package com.kimo.reverprint.useCases.tinyprint.units

import com.kimo.reverprint.useCases.tinyprint.DeviceChecker
import com.kimo.reverprint.useCases.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.tools.bluetooth.BluetoothController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BluetoothDeviceChecker(
    scope: CoroutineScope,
    bluetoothController: BluetoothController,
    protocol: DeviceCommunicationProtocol
): DeviceChecker {

    private val isFull = bluetoothController.read()
        .map { protocol.parseReceivedMessage(it) }
        .filterIsInstance<DeviceCommunicationProtocol.DeviceAnswer.IsFull>()
        .map { it.isFull }
        .stateIn(scope, SharingStarted.Companion.Eagerly, false)

    override suspend fun suspendIfOverloaded() {
        if (isFull.value) isFull
            .also { println("BluetoothDeviceChecker: Suspending...") }
            .filter { !it }
            .first()
            .also { println("BluetoothDeviceChecker: Device is free again!") }
    }
}