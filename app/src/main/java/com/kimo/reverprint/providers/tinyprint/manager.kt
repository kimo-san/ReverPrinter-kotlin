package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.printer.DeviceManager
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.domain.printer.PrintMode
import com.kimo.reverprint.domain.printer.ThermalPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TinyprintManager(
    val deviceController: TinyprintBluetoothController,
    val previewGenerator: PreviewGenerator
) : DeviceManager {

    private val deviceGetter: () -> ThermalPrinter =
        { deviceController.pairedDevice.value ?: error("Disconnected from device...") }

    val protocol get() = deviceController.protocol

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
    ): DeviceManager.PrintPreviews = withContext(Dispatchers.Default) {
        previewGenerator.generate(imageBitmap, printConfig, deviceGetter)
    }

    override suspend fun print(
        imagePreview: DeviceManager.PrintPreviews,
        mode: PrintMode
    ) = deviceController.stream {
        TinyprintPrinter(
            protocol = protocol,
            sender = { send(it) }
        ).print(imagePreview, mode)
    }
}