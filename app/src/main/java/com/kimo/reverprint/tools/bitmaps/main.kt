package com.kimo.reverprint.tools.bitmaps

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.bitmaps.impl.BitmapProcessorImpl

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
    val modified = generateModifiedPixels(source, settings)
    returnValues(modified, generateViewablePixels(modified))
}

fun MutablePixels(
    pixelsArray: IntArray,
    width: Int,
    height: Int,
    model: DomainColorModel
) = MutablePixels(pixelsArray, width, height, model.implementedEquivalent())

fun ImmutablePixels(
    intPixels: IntArray,
    width: Int,
    height: Int,
    model: DomainColorModel
) = ImmutablePixels(MutablePixels(intPixels, width, height, model))

private fun MutablePixels.asImmutable() = ImmutablePixels(this)

private suspend fun generateModifiedPixels(source: ImagePixels, settings: BitmapSettings) =
    BitmapProcessorImpl(asMutablePixels(source), settings)
        .processPixels()
        .asImmutable()

private suspend fun generateViewablePixels(source: ImagePixels) =
    BitmapProcessorImpl(
        asMutablePixels(source),
        BitmapSettings(colorModel = DomainColorModel.ARGB_8)
    ).processPixels().asImmutable()

private fun asMutablePixels(bitmap: ImagePixels): MutablePixels {
    return bitmap as? MutablePixels ?: MutablePixels(
        pixelsArray = bitmap.pixelList.toIntArray(),
        width = bitmap.width,
        height = bitmap.height,
        model = DomainColorModel.ARGB_8
    )
}