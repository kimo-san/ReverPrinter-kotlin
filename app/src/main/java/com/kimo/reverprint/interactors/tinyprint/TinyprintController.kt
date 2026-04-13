package com.kimo.reverprint.interactors.tinyprint

import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.interactors.tinyprint.units.BluetoothDeviceChecker
import com.kimo.reverprint.interactors.tinyprint.units.PreviewGenerator
import com.kimo.reverprint.interactors.tinyprint.units.Printer
import com.kimo.reverprint.interactors.tinyprint.units.TinyprintDevice
import com.kimo.reverprint.tools.bluetooth.BleCharacteristic
import com.kimo.reverprint.tools.bluetooth.BluetoothDevice
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class TinyprintController(
    val bluetoothController: BluetoothLeController,
    val protocol: DeviceCommunicationProtocol
) : DeviceController {

    private var pairedToDevice = MutableStateFlow<TinyprintDevice?>(null)
    private val deviceGetter =
        { pairedToDevice.value ?: error("Disconnected from device...") }

    override val connectedTo: Flow<ThermalPrinter?> get() = pairedToDevice
        .filterNotNull()
        .combine(bluetoothController.connectedToDevice) { pair, dev ->
            dev?.let {
                ThermalPrinter(
                    name = pair.modelNo,
                    macAddress = dev.address,
                    supportedModes = buildList {
                        if (pair.isGrayPrint) add(PrintMode.BPP4)
                        add(PrintMode.BPP1)
                    }
                )
            }
        }

    override suspend fun connect(device: ThermalPrinter) {
        bluetoothController.apply {
            connect(device.asBluetoothDevice())
            setReadCharacteristic(BleCharacteristic(READ_UUID))
            setWriteCharacteristic(BleCharacteristic(WRITE_UUID))
        }
        pairedToDevice.update { TinyprintDevice.supportedPrinters[device.name] }
        protocol.deviceGetter(deviceGetter)
    }

    override suspend fun disconnect() {
        bluetoothController.disconnect()
        pairedToDevice.update { null }
    }

    override fun findAvailable(): Flow<ThermalPrinter> = channelFlow {
        bluetoothController.discovery()
            .map { dev ->
                val found = TinyprintDevice.supportedPrinters.entries.find { (_, v) ->
                    dev.name?.startsWith(v.headName) == true
                } ?: return@map null
                Triple(found.key, found.value, dev)
            }
            .filterNotNull()
            .onEach { (modelName, tinyprintModel, bleDevice) ->
                send(
                    ThermalPrinter(
                        name = modelName,
                        macAddress = bleDevice.address,
                        supportedModes = buildList {
                            if (tinyprintModel.isGrayPrint) add(PrintMode.BPP4)
                            add(PrintMode.BPP1)
                        }
                    )
                )
            }.collect()
    }

    override suspend fun generatePreviews(
        imageBitmap: ImagePixels,
        printConfig: DeviceController.PrintConfig
    ): DeviceController.PrintPreviews {
        val result = PreviewGenerator(deviceGetter)
            .generate(imageBitmap, printConfig)
        return result
    }

    override suspend fun print(
        imagePreview: DeviceController.PrintPreviews,
        mode: PrintMode
    ) = coroutineScope {
        Printer(
            deviceGetter = deviceGetter,
            protocol = protocol,
            sendBytes = { bluetoothController.send(it) },
            deviceChecker = { BluetoothDeviceChecker(this, bluetoothController, protocol) },
        ).print(imagePreview, mode)
    }

    private fun ThermalPrinter.asBluetoothDevice() = BluetoothDevice(
        name = null,
        address = macAddress
    )

}

private const val WRITE_UUID = "0000AE01-0000-1000-8000-00805F9B34FB"
private const val READ_UUID =  "0000AE02-0000-1000-8000-00805F9B34FB"