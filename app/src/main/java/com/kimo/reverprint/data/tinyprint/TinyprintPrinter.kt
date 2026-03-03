package com.kimo.reverprint.data.tinyprint

import android.content.Context
import android.graphics.Bitmap
import com.kimo.reverprint.data.bluetooth.AndroidBluetoothLeController
import com.kimo.reverprint.data.bluetooth.BleCharacteristic
import com.kimo.reverprint.data.bluetooth.BluetoothController
import com.kimo.reverprint.data.bluetooth.BluetoothDevice
import com.kimo.reverprint.data.imageProcessing.BitmapProcessor
import com.kimo.reverprint.data.tinyprint.TinyprintPrinterBean.Companion.supportedPrinters
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.domain.ThermalPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TinyprintPrinter(
    private val context: Context,
    private val bitmapProcessor: BitmapProcessor
): Printer {

    override val connectedTo: MutableStateFlow<ThermalPrinter?> = MutableStateFlow(null)
    private var pairedToPrinter: TinyprintPrinterBean? = null
    private val bluetoothController: BluetoothController by lazy {
        AndroidBluetoothLeController(
            context = context,
            inputCharacteristic = BleCharacteristic(
                UUID.fromString("0000AE01-0000-1000-8000-00805F9B34FB")
            )
        )
    }

    override fun findAvailable(): Flow<ThermalPrinter> = channelFlow {

        bluetoothController.discover()
            .map { dev ->

                val found = supportedPrinters.entries.find { (_, v) ->
                    dev.name?.startsWith(v.headName) == true
                } ?: return@map null

                Triple(found.key, found.value, dev)
            }
            .filterNotNull()
            .onEach { (modelName, tpModel, bleDevice) ->

                send(ThermalPrinter(
                    name = modelName,
                    macAddress = bleDevice.address,
                    supportedModes = buildList {
                        if (tpModel.isGrayPrint)
                            add(PrintMode.BPP4)
                        add(PrintMode.BPP1)
                    }
                ))
            }.collect()
    }

    override suspend fun connect(device: ThermalPrinter) {
        bluetoothController.connect(device.asBluetoothDevice())
        connectedTo.update { device }
        pairedToPrinter = supportedPrinters[device.name]
    }

    override suspend fun generatePreviews(imageBitmap: Bitmap): Printer.PrintPreviews = withContext(Dispatchers.Default) {

        val printer = pairedToPrinter ?: error("Not connected to printer")
        val previews = mutableMapOf<PrintMode, Bitmap>()

        coroutineScope {
            launch {
                previews[PrintMode.BPP1] = bitmapProcessor.process(imageBitmap) {
                    resize(printer.printSize)
//                    saturation(0.5f)
//                    depth(4)
//                    dither()
                }
            }
        }


        Printer.PrintPreviews(previews)
    }

    override suspend fun print(
        imagePreview: Printer.PrintPreviews,
        mode: PrintMode
    ) {
        TODO()
    }

    override suspend fun disconnect() {
        bluetoothController.disconnect()
        connectedTo.update { null }
        pairedToPrinter = null
    }

    private fun ThermalPrinter.asBluetoothDevice() = BluetoothDevice(
        name = null, // the name may not match the real device name
        address = macAddress
    )
}