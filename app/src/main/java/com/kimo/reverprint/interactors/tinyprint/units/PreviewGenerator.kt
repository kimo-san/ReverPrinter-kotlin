package com.kimo.reverprint.interactors.tinyprint.units

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.interactors.tinyprint.PrintPreview
import com.kimo.reverprint.interactors.bitmapPlayground.convertViewableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewGenerator(
    val deviceGetter: () -> TinyprintDevice
) {

    suspend fun generate(
        imageBitmap: ImagePixels,
        printConfig: DeviceController.PrintConfig
    ): DeviceController.PrintPreviews =
        withContext(Dispatchers.Default) {

            val previews = mutableMapOf<PrintMode, ImagePixels>()
            val toPrint = mutableMapOf<PrintMode, ImagePixels>()

            launch {
                generate1bppPreview(imageBitmap, printConfig) { printable, preview ->
                    previews[PrintMode.BPP1] = preview
                    toPrint[PrintMode.BPP1] = printable
                }
            }

            launch {
                generate4bppPreview(imageBitmap, printConfig) { printable, preview ->
                    previews[PrintMode.BPP4] = preview
                    toPrint[PrintMode.BPP4] = printable
                }
            }

            joinAll()
            PrintPreview(previews, toPrint, printConfig)
        }

    private suspend fun generate4bppPreview(
        imageBitmap: ImagePixels,
        printConfig: DeviceController.PrintConfig,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = deviceGetter().printSize,
                colorModel = ColorModel.GREY_4BPP,
                dither = printConfig.ditherImage
            )
        ) { modified, viewable ->
            returnValues(modified, viewable)
        }
    }

    private suspend fun generate1bppPreview(
        imageBitmap: ImagePixels,
        printConfig: DeviceController.PrintConfig,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = deviceGetter().printSize,
                colorModel = ColorModel.MONO,
                dither = printConfig.ditherImage
            )
        ) { modified, viewable ->
            returnValues(modified, viewable)
        }
    }
}