package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.DeviceManager
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import kotlinx.coroutines.flow.Flow

class TinyprintManager(
    bluetoothController: BluetoothLeController
) : DeviceManager {

    val deviceController = TinyprintBluetoothController(bluetoothController)
    val protocol get() = deviceController.protocol
    private val deviceGetter: () -> ThermalPrinter =
        { deviceController.pairedDevice.value ?: error("Disconnected from device...") }

    override val connectedTo: Flow<ThermalPrinter?>
        get() = deviceController.pairedDevice

    override suspend fun connect(device: ThermalPrinter) =
        deviceController.connect(device)

    override suspend fun disconnect() =
        deviceController.disconnect()

    override fun findAvailable(): Flow<ThermalPrinter> =
        deviceController.discovery()

    override suspend fun generatePreviews(
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig
    ): DeviceManager.PrintPreviews {
        return PreviewGenerator(deviceGetter)
            .generate(imageBitmap, printConfig)
    }

    override suspend fun print(
        imagePreview: DeviceManager.PrintPreviews,
        mode: PrintMode
    ) = deviceController.stream {
        println("Start printing...")
        TinyprintPrinter(
            protocol = deviceController.protocol,
            sendBytes = { send(it) }
        ).print(imagePreview, mode)
    }
}