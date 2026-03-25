package com.kimo.reverprint.useCases.tinyprint.units

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.useCases.tinyprint.PrintPreview
import com.kimo.reverprint.useCases.tinyprint.TinyprintDevice
import com.kimo.reverprint.tools.bitmaps.convertViewableImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewGenerator(
    val device: TinyprintDevice
) {

    suspend fun generate(
        imageBitmap: ImagePixels,
        configuration: DeviceController.Configuration
    ): DeviceController.PrintPreviews =
        withContext(Dispatchers.Default) {

            val previews = mutableMapOf<PrintMode, ImagePixels>()
            val toPrint = mutableMapOf<PrintMode, ImagePixels>()

            launch {
                generate1bppPreview(imageBitmap, configuration) { printable, preview ->
                    previews[PrintMode.BPP1] = preview
                    toPrint[PrintMode.BPP1] = printable
                }
            }

            launch {
                generate4bppPreview(imageBitmap, configuration) { printable, preview ->
                    previews[PrintMode.BPP4] = preview
                    toPrint[PrintMode.BPP4] = printable
                }
            }

            joinAll()
            PrintPreview(previews, toPrint, configuration)
        }

    private suspend fun generate4bppPreview(
        imageBitmap: ImagePixels,
        configuration: DeviceController.Configuration,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = device.printSize,
                colorModel = ColorModel.GREY_4BPP,
                dither = configuration.ditherImage
            ),
            returnValues
        )
    }

    private suspend fun generate1bppPreview(
        imageBitmap: ImagePixels,
        configuration: DeviceController.Configuration,
        returnValues: (
            printable: ImagePixels,
            preview: ImagePixels
        ) -> Unit
    ) {
        convertViewableImage(
            imageBitmap,
            BitmapSettings(
                width = device.printSize,
                colorModel = ColorModel.MONO,
                dither = configuration.ditherImage
            ),
            returnValues
        )
    }
}