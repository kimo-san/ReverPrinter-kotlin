package com.kimo.reverprint.extensions.bitmaps

import com.kimo.reverprint.domain.images.BitmapSettings
import com.kimo.reverprint.domain.images.ColorModel
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.changeColorModel
import com.kimo.reverprint.tools.graphics.resizedCopy

interface BitmapConverter {

    /**
     * `Viewable` means an image of 8-bit ARGB color format
     * @param returnValues modified bitmap and then viewable preview
     */
    suspend fun convertViewableImage(
        source: ImagePixels,
        settings: BitmapSettings,
        returnValues: (
            modified: ImagePixels,
            viewable: ImagePixels
        ) -> Unit
    )
}


class BitmapConverterImpl(
    val creator: BitmapCreator
): BitmapConverter {

    override suspend fun convertViewableImage(
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

    private suspend fun generateModifiedPixels(source: ImagePixels, settings: BitmapSettings): Pixels {

        val bp = creator.createFastExtendable(
            source.width,
            source.model.implementedEquivalent()
        ).insertFrom(source)

        return BitmapProcessorImpl(
            bp, settings, creator
        ).process()
    }

    private suspend fun generateViewablePixels(source: ImagePixels): Pixels {
        return BitmapProcessorImpl(
            creator.createFastExtendable(
                source.width,
                source.model.implementedEquivalent()
            ).insertFrom(source),
            BitmapSettings(colorModel = ColorModel.ARGB_8),
            creator
        ).process()
    }
}

private class BitmapProcessorImpl(
    image: Pixels,
    val settings: BitmapSettings,
    val creator: BitmapCreator
) {

    private var pixels: Pixels = image
    suspend fun process(): Pixels {
        pixels = pixels.resizedCopy(settings.width, settings.height, creator)
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