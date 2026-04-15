package com.kimo.reverprint.tools.bluetooth

interface BluetoothLeController: BluetoothController {
    fun setWriteCharacteristic(txCharacteristic: BluetoothLeCharacteristic?)
    fun setReadCharacteristic(rxCharacteristic: BluetoothLeCharacteristic?)
}