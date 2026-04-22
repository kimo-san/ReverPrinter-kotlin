package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.images.BitmapSettings
import com.kimo.reverprint.domain.images.ColorModel
import com.kimo.reverprint.domain.printer.DeviceManager
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.domain.printer.PrintMode
import com.kimo.reverprint.domain.printer.ThermalPrinter
import com.kimo.reverprint.extensions.bitmaps.BitmapConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrintPreview(
    viewable: Map<PrintMode, ImagePixels>,
    val printable: Map<PrintMode, ImagePixels>,
    printConfig: DeviceManager.PrintConfig,
) : DeviceManager.PrintPreviews(viewable, printConfig)

class PreviewGenerator(val converter: BitmapConverter) {

    lateinit var deviceGetter: () -> ThermalPrinter
    suspend fun generate(
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig,
        deviceGetter: () -> ThermalPrinter,
    ): PrintPreview =
        withContext(Dispatchers.Default) {

            this@PreviewGenerator.deviceGetter = deviceGetter
            val previews = mutableMapOf<PrintMode, ImagePixels>()
            val toPrint = mutableMapOf<PrintMode, ImagePixels>()

            deviceGetter().capabilities.supportedModes.forEach {
                launch {
                    distributeAndGeneratePreview(
                        it,
                        imageBitmap,
                        printConfig
                    ) { printable, preview ->
                        previews[it] = preview
                        toPrint[it] = printable
                    }
                }
            }

            joinAll()
            PrintPreview(previews, toPrint, printConfig)
        }

    private suspend fun distributeAndGeneratePreview(
        mode: PrintMode,
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        when (mode) {
            PrintMode.BPP1 -> generate1bppPreview(imageBitmap, printConfig, returnValues)
            PrintMode.BPP4 -> generate4bppPreview(imageBitmap, printConfig, returnValues)
        }
    }

    private suspend fun generate4bppPreview(
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        converter.convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = deviceGetter().capabilities.printWidth,
                colorModel = ColorModel.GREY_4BPP,
                dither = printConfig.ditherImage
            )
        ) { modified, viewable ->
            returnValues(modified, viewable)
        }
    }

    private suspend fun generate1bppPreview(
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        converter.convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = deviceGetter().capabilities.printWidth,
                colorModel = ColorModel.MONO,
                dither = printConfig.ditherImage
            )
        ) { modified, viewable ->
            returnValues(modified, viewable)
        }
    }
}