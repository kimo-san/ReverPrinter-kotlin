package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.math.roundToInt

class BitmapProcessorImpl : BitmapProcessor {

    private lateinit var pixels: Pixels
    private lateinit var settings: BitmapSettings

    override fun setSettings(settings: BitmapSettings) {
        this.settings = settings
    }

    override fun setImage(bitmap: Bitmap) {
        val bm = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        pixels = Pixels(bm)
    }

    override suspend fun processPixels(): Pixels {

        resize(settings?.width, settings?.height)
        settings?.colorModel?.let {
            changeColorModel(it)
        }

        return pixels.copy()
    }

    suspend fun changeColorModel(newModel: ColorModel) = withContext(Dispatchers.Default) {
        when (newModel) {
            pixels.model -> Unit
            is Monochrome -> {
                if (settings.dither) {
                    pixels.toGrey8bpp()
                    fitToModelUsingDithering(pixels, Monochrome)
                } else {
                    pixels.toMonochrome()
                }

            }
            is Grey8bpp -> pixels.toGrey8bpp()
            is Grey4bpp -> {
                if (settings.dither && pixels.model.channelDepth > Grey4bpp.channelDepth) {
                    pixels.toGrey8bpp()
                    fitToModelUsingDithering(pixels, Grey4bpp)
                } else {
                    pixels.toGrey4bpp()
                }
            }


            is Argb -> {
                when (pixels.model) {

                    is Monochrome -> {

                        pixels.forEach { p, x, y ->
                            val bwColor = p[x, y] * (Argb.channelDepth - 1)

                            p[x, y] = Argb.colorOf(
                                a = Argb.channelDepth - 1,
                                r = bwColor,
                                g = bwColor,
                                b = bwColor
                            ).int
                        }
                    }

                    is Grey4bpp -> {
                        pixels.forEach { p, x, y ->

                            val grayColor = (p.model
                                .colorOf(p[x, y])
                                .lum() * (newModel.channelDepth - 1))
                                .fastRoundToInt()

                            p[x, y] = newModel.colorOf(
                                a = newModel.channelDepth - 1,
                                r = grayColor,
                                g = grayColor,
                                b = grayColor
                            ).int
                        }
                    }

                    else -> error("Unsupported")
                }
                pixels.model = Argb
            }

            else -> error("Unsupported")
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

        val scaledBitmap = Pixels(
            arr = IntArray(newWidth * newHeight),
            width = newWidth,
            height = newHeight,
            model = pixels.model
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
        pixels: Pixels,
        newModel: ColorModel
    ): Unit = withContext(Dispatchers.Default) {
        require(newModel.channelCount == 1) {
            "Dithering is currently applicable only for 1-channel colors. Current model is ${pixels.model::class.simpleName}"
        }

        val maxLevel = max(newModel.channelDepth - 1, 1)

        val step = pixels.model.channelDepth / maxLevel
        pixels.forEach { p, x, y ->

            val oldPixelValue = p[x, y]
            val newPixelValue = (oldPixelValue *1f / step).roundToInt() * step
            val error = oldPixelValue - newPixelValue

            p[x    , y    ] = newPixelValue
            p[x + 1, y    ] = (p[x + 1, y    ] + error * 7f / 16).toInt()
            p[x - 1, y + 1] = (p[x - 1, y + 1] + error * 3f / 16).toInt()
            p[x    , y + 1] = (p[x    , y + 1] + error * 5f / 16).toInt()
            p[x + 1, y + 1] = (p[x + 1, y + 1] + error * 1f / 16).toInt()
        }

        pixels.forEach { p, x, y ->
            p[x, y] = (p[x, y] / step).coerceIn(0..maxLevel)
        }

        pixels.forEach { p, x, y ->
            if (p[x, y] !in 0..maxLevel) { "Ouf of bounds: ${p[x, y]}".let { println(it) } }
        }
        require(pixels.arr.all { it in 0..maxLevel })
        pixels.model = newModel
    }

}