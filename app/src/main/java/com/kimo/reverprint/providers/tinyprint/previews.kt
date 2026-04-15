package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.DeviceManager
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.ThermalPrinter
import com.kimo.reverprint.interactors.bitmaps.convertViewableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrintPreview(
    viewable: Map<PrintMode, ImagePixels>,
    val printable: Map<PrintMode, ImagePixels>,
    printConfig: DeviceManager.PrintConfig,
) : DeviceManager.PrintPreviews(viewable, printConfig)

class PreviewGenerator(
    val deviceGetter: () -> ThermalPrinter
) {

    suspend fun generate(
        imageBitmap: ImagePixels,
        printConfig: DeviceManager.PrintConfig
    ): PrintPreview =
        withContext(Dispatchers.Default) {

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
        convertViewableImage(
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
        convertViewableImage(
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