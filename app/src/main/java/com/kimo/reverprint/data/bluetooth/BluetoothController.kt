package com.kimo.reverprint.data.bluetooth

import kotlinx.coroutines.flow.Flow

interface BluetoothController {

    val connectedToDevice: BluetoothDevice?
    fun discover(): Flow<IBluetoothDevice>
    suspend fun connect(device: BluetoothDevice)
    suspend fun send(data: ByteArray)
    fun disconnect()

}