package com.kimo.reverprint.di

import android.content.Context
import com.kimo.reverprint.data.bluetooth.AndroidBluetoothLeController
import com.kimo.reverprint.data.bluetooth.BleCharacteristic
import com.kimo.reverprint.data.tinyprint.ProtocolImpl
import com.kimo.reverprint.data.tinyprint.TinyprintController

fun initializeTinyprintController(context: Context) =
    TinyprintController(
        context = context,
        bluetoothController = with(TinyprintController.BleCharacteristics) {
            AndroidBluetoothLeController(
                context,
                BleCharacteristic(WRITE_UUID),
                BleCharacteristic(READ_UUID)
            )
        },
        protocol = ProtocolImpl()
    )
