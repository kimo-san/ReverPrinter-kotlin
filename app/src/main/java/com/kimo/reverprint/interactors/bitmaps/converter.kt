package com.kimo.reverprint.interactors.bitmaps

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.graphics.Pixels

/**
 * `Viewable` means image of 8-bit ARGB color format
 * @param returnValues modified bitmap and then viewable preview
 */
suspend fun convertViewableImage(
    source: ImagePixels,
    settings: BitmapSettings,
    returnValues: (
        modified: ImagePixels,
        viewable: ImagePixels
    ) -> Unit
) {
    val modified = generateModifiedPixels(source, settings).asDomainImmutable()
    val viewable = generateViewablePixels(modified).asDomainImmutable()
    returnValues(modified, viewable)
}

private suspend fun generateModifiedPixels(source: ImagePixels, settings: BitmapSettings) =
    BitmapProcessorImpl(Pixels(source), settings)
        .processPixels()

private suspend fun generateViewablePixels(source: ImagePixels) =
    BitmapProcessorImpl(
        Pixels(source),
        BitmapSettings(colorModel = ColorModel.ARGB_8)
    ).processPixels()

private class BitmapProcessorImpl(
    image: Pixels,
    val settings: BitmapSettings
) {

    private var pixels: Pixels = image
    suspend fun processPixels(): Pixels {
        pixels = pixels.resized(settings.width, settings.height)
        if (settings.colorModel != null) {
            changeColorModel(
                settings.colorModel.implementedEquivalent(),
                pixels = pixels,
                dither = settings.dither
            )
        }
        return pixels.getCopy()
    }
}