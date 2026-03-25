package com.kimo.reverprint.tools.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val connectedToDevice: StateFlow<BluetoothDevice?>
    fun discovery(): Flow<BluetoothDevice>
    suspend fun connect(device: BluetoothDevice)
    suspend fun send(data: ByteArray)
    fun read(): Flow<ByteArray>
    fun disconnect()

}

