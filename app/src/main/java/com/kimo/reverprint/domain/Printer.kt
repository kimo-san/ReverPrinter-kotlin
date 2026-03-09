package com.kimo.reverprint.domain

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Printer {

    val connectedTo: StateFlow<ThermalPrinter?>
    fun findAvailable(): Flow<ThermalPrinter>
    suspend fun connect(device: ThermalPrinter)
    suspend fun disconnect()

    suspend fun generatePreviews(imageBitmap: Bitmap): PrintPreviews
    suspend fun print(imagePreview: PrintPreviews, mode: PrintMode)

    abstract class PrintPreviews(
        private val availableModes: Map<PrintMode, Bitmap>
    ) { operator fun get(mode: PrintMode) = availableModes[mode] }
}