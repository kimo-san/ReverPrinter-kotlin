package com.kimo.reverprint.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface Printer {

    val connectedTo: Flow<ThermalPrinter?>
    fun findAvailable(): Flow<ThermalPrinter>
    suspend fun connect(device: ThermalPrinter)
    suspend fun disconnect()

    suspend fun generatePreviews(
        imageBitmap: Bitmap,
        configuration: Configuration
    ): PrintPreviews
    suspend fun print(
        imagePreview: PrintPreviews,
        mode: PrintMode
    )

    abstract class PrintPreviews(
        private val availableModes: Map<PrintMode, Bitmap>,
        val appliedConfiguration: Configuration
    ) { operator fun get(mode: PrintMode) = availableModes[mode] }

    class Configuration(
        val addSpaceAfterPrint: Boolean = true,
        val ditherImage: Boolean = false
    )
}