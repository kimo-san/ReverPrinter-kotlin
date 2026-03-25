package com.kimo.reverprint.tools.bluetooth

interface BluetoothLeController: BluetoothController {
    fun setWriteCharacteristic(txCharacteristic: BleCharacteristic?)
    fun setReadCharacteristic(rxCharacteristic: BleCharacteristic?)
}