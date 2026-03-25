package com.kimo.reverprint.useCases.tinyprint

import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.useCases.tinyprint.units.BluetoothDeviceChecker
import com.kimo.reverprint.useCases.tinyprint.units.PreviewGenerator
import com.kimo.reverprint.useCases.tinyprint.units.Printer
import com.kimo.reverprint.tools.bluetooth.BleCharacteristic
import com.kimo.reverprint.tools.bluetooth.BluetoothDevice
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

class TinyprintController(
    val bluetoothController: BluetoothLeController,
    val protocol: DeviceCommunicationProtocol
) : DeviceController {

    init {
        bluetoothController.setReadCharacteristic(BleCharacteristic(READ_UUID))
        bluetoothController.setWriteCharacteristic(BleCharacteristic(WRITE_UUID))
        protocol.deviceGetter { pairedToDevice.value }
    }

    private var pairedToDevice = MutableStateFlow<TinyprintDevice?>(null)

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
        bluetoothController.connect(device.asBluetoothDevice())
        pairedToDevice.update { TinyprintDevice.supportedPrinters[device.name] }
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
        configuration: DeviceController.Configuration
    ): DeviceController.PrintPreviews {
        return PreviewGenerator(
            device = pairedToDevice.value ?: error("Disconnected from device...")
        ).generate(imageBitmap, configuration)
    }

    override suspend fun print(
        imagePreview: DeviceController.PrintPreviews,
        mode: PrintMode
    ) = coroutineScope {
        Printer(
            getPairedDevice = { pairedToDevice.value ?: error("Disconnected from device...") },
            protocol = protocol,
            sendBytes = { bluetoothController.send(it) },
            getDeviceChecker = { BluetoothDeviceChecker(this, bluetoothController, protocol) },
        ).print(imagePreview, mode)
    }

    private fun ThermalPrinter.asBluetoothDevice() = BluetoothDevice(
        name = null,
        address = macAddress
    )

}

private const val WRITE_UUID = "0000AE01-0000-1000-8000-00805F9B34FB"
private const val READ_UUID =  "0000AE02-0000-1000-8000-00805F9B34FB"