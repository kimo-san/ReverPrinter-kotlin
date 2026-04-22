package com.kimo.reverprint.domain.printer

import com.kimo.reverprint.domain.images.ImagePixels
import kotlinx.coroutines.flow.Flow

interface DeviceManager {

    val connectedTo: Flow<ThermalPrinter?>
    fun findAvailable(): Flow<ThermalPrinter>
    suspend fun connect(device: ThermalPrinter)
    suspend fun disconnect()

    suspend fun generatePreviews(
        imageBitmap: ImagePixels,
        printConfig: PrintConfig
    ): PrintPreviews

    suspend fun print(
        imagePreview: PrintPreviews,
        mode: PrintMode
    )

    abstract class PrintPreviews(
        private val availableModes: Map<PrintMode, ImagePixels>,
        val appliedPrintConfig: PrintConfig
    ) { operator fun get(mode: PrintMode) = availableModes[mode] }

    class PrintConfig(
        val addSpaceAfterPrint: Boolean = true,
        val ditherImage: Boolean = false
    )
}