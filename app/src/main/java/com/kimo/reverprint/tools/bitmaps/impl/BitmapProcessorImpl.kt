package com.kimo.reverprint.tools.bitmaps.impl

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.bitmaps.BitmapProcessor
import com.kimo.reverprint.tools.bitmaps.ColorModel
import com.kimo.reverprint.tools.bitmaps.MutablePixels
import com.kimo.reverprint.tools.bitmaps.implementedEquivalent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class BitmapProcessorImpl(
    image: MutablePixels,
    override val settings: BitmapSettings
) : BitmapProcessor {

    private var pixels: MutablePixels = image
    override val image: ImagePixels get() = pixels

    override suspend fun processPixels(): MutablePixels {

        resize(settings.width, settings.height)
        settings.colorModel?.let {
            changeColorModel(it.implementedEquivalent())
        }

        return pixels.copy()
    }

    suspend fun changeColorModel(newModel: ColorModel) = withContext(Dispatchers.Default) {
        when (newModel) {
            pixels.implementedModel -> Unit

            is Argb8 -> pixels.toArgb8()
            is Grey8 -> pixels.toGrey8bpp()

            is Grey4 -> {
                if (settings.dither && pixels.implementedModel.channelDepth > Grey4.channelDepth) {
                    pixels.toGrey8bpp()
                    fitToModelUsingDithering(pixels, Grey4)
                } else {
                    pixels.toGrey4bpp()
                }
            }

            is Monochrome -> {
                if (settings.dither) {
                    pixels.toGrey8bpp()
                    fitToModelUsingDithering(pixels, Monochrome)
                } else {
                    pixels.toMonochrome()
                }
            }

            else -> error("Unsupported color model ${newModel::class.simpleName}")
        }
    }

    suspend fun resize(width: Int?, height: Int?): Unit = withContext(Dispatchers.Default) {

        val (originalWidth, originalHeight) = pixels.width to pixels.height
        val newWidth: Int
        val newHeight: Int

        when {
            (width == null && height != null) -> {
                newWidth = (originalWidth * height / originalHeight).coerceAtLeast(1)
                newHeight = height
            }

            (width != null && height == null) -> {
                newWidth = width
                newHeight = (originalHeight * width / originalWidth).coerceAtLeast(1)
            }

            else -> {
                newWidth = width ?: return@withContext
                newHeight = height ?: return@withContext
            }
        }

        val scaledBitmap = MutablePixels(
            pixelsArray = IntArray(newWidth * newHeight),
            width = newWidth,
            height = newHeight,
            implementedModel = pixels.implementedModel
        )
        val (scaleX, scaleY) = (originalWidth.toFloat() / newWidth) to (originalHeight.toFloat() / newHeight)
        scaledBitmap.forEach { p, x, y ->
            p[x, y] = pixels[
                (x * scaleX).toInt(),
                (y * scaleY).toInt()
            ]
        }

        pixels = scaledBitmap
    }

    suspend fun fitToModelUsingDithering(
        pixels: MutablePixels,
        newModel: ColorModel
    ): Unit = withContext(Dispatchers.Default) {
        require(newModel.channelCount == 1) {
            "Dithering is currently applicable only for 1-channel colors. Current model is ${pixels.implementedModel::class.simpleName}"
        }

        val maxLevel = max(newModel.channelDepth - 1, 1)

        val step = pixels.implementedModel.channelDepth / maxLevel
        pixels.forEach { p, x, y ->

            val oldPixelValue = p[x, y]
            val newPixelValue = (oldPixelValue * 1f / step).roundToInt() * step
            val error = oldPixelValue - newPixelValue

            p[x, y] = newPixelValue
            p[x + 1, y] = (p[x + 1, y] + error * 7f / 16).toInt()
            p[x - 1, y + 1] = (p[x - 1, y + 1] + error * 3f / 16).toInt()
            p[x, y + 1] = (p[x, y + 1] + error * 5f / 16).toInt()
            p[x + 1, y + 1] = (p[x + 1, y + 1] + error * 1f / 16).toInt()
        }

        pixels.forEach { p, x, y ->
            p[x, y] = (p[x, y] / step).coerceIn(0..maxLevel)
        }

        pixels.forEach { p, x, y ->
            if (p[x, y] !in 0..maxLevel) {
                println("Ouf of bounds: ${p[x, y]}")
            }
        }
        require(pixels.pixelList.all { it in 0..maxLevel })
        pixels.implementedModel = newModel
    }
}