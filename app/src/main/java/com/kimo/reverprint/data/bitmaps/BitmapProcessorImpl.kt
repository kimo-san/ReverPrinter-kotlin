package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class BitmapProcessorImpl : BitmapProcessor {

    private var pixels: Pixels = Pixels(intArrayOf(0), 1, 1)
    private var currentColorModel by Delegates.notNull<ColorModel>()
    private var settings: BitmapSettings? = null

    override fun setSettings(settings: BitmapSettings) {
        this.settings = settings
    }

    override fun setImage(bitmap: Bitmap) {
        val bm = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        pixels = Pixels(bm)
        currentColorModel = Argb
    }

    override suspend fun processPixels(): Pixels {

        resize(settings?.width, settings?.height)
        settings?.dither?.takeIf { it }?.let {
            dither()
        }
        settings?.colorModel?.let {
            changeColorModel(it)
        }

        return pixels.copy()
    }

    suspend fun changeColorModel(newModel: ColorModel) = withContext(Dispatchers.Default) {
        when (newModel) {

            is Grey4bpp -> {
                if (currentColorModel !is Grey4bpp)
                    pixels.forEach { p, x, y ->
                        val lum = currentColorModel.colorOf(p[x, y]).lum() *
                                (newModel.channelDepth - 1)
                        p[x, y] = newModel.colorOf(lum.roundToInt()).int
                    }
            }

            is Monochrome -> {
                pixels.forEach { p, x, y ->
                    val lum = currentColorModel.colorOf(p[x, y]).lum() *
                            (newModel.channelDepth - 1)

                    p[x, y] = newModel.colorOf(lum.fastRoundToInt()).int
                }
            }

            is Argb -> {
                when (currentColorModel) {

                    is Monochrome -> {

                        pixels.forEach { p, x, y ->
                            val bwColor = currentColorModel.colorOf(p[x, y])[Monochrome.W] *
                                    (newModel.channelDepth - 1)

                            p[x, y] = newModel.colorOf(
                                a = newModel.channelDepth - 1,
                                r = bwColor,
                                g = bwColor,
                                b = bwColor
                            ).int
                        }
                    }

                    is Grey4bpp -> {
                        pixels.forEach { p, x, y ->

                            val grayColor = (currentColorModel
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

                    is Argb -> {  }
                    else -> error("Unsupported")
                }
            }

            else -> error("Unsupported")
        }
        currentColorModel = newModel
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

        val scaledBitmap = Pixels(IntArray(newWidth * newHeight), newWidth, newHeight)
        val (scaleX, scaleY) = (originalWidth.toFloat() / newWidth) to (originalHeight.toFloat() / newHeight)
        scaledBitmap.forEach { p, x, y ->
            p[x, y] = pixels[
                (x * scaleX).toInt(),
                (y * scaleY).toInt()
            ]
        }

        pixels = scaledBitmap
    }

    suspend fun dither(): Unit = withContext(Dispatchers.Default) {

        fun findClosestColor(color: ColorOfModel): ColorOfModel =
            color.remapped { channel, value ->
                val quantize = currentColorModel.channelDepth - 1f
                set(channel, (value / quantize * quantize).fastRoundToInt())
            }

        fun applyDifference(
            color: ColorOfModel,
            errors: IntArray,
            factor: Float
        ): ColorOfModel =
            color.remapped { channel, newValue ->
                set(channel, (color[channel] + errors[channel] * factor).roundToInt())
            }

        pixels.forEach { p, x, y ->

            val oldPixel = currentColorModel.colorOf(p[x, y])
            val newPixel = findClosestColor(currentColorModel.colorOf(p[x, y]))

            val errorsOnChannels = IntArray(oldPixel.model.channelCount) { oldPixel[it] - newPixel[it] }
            p[x    , y    ] = newPixel.int
            p[x + 1, y    ] = applyDifference(currentColorModel.colorOf(p[x + 1, y]), errorsOnChannels, 7f / 16).int
            p[x - 1, y + 1] = applyDifference(currentColorModel.colorOf(p[x - 1, y + 1]), errorsOnChannels, 3f / 16).int
            p[x    , y + 1] = applyDifference(currentColorModel.colorOf(p[x, y + 1]), errorsOnChannels, 5f / 16).int
            p[x + 1, y + 1] = applyDifference(currentColorModel.colorOf(p[x + 1, y + 1]), errorsOnChannels, 1f / 16).int
        }
    }

    private suspend fun Pixels.forEach(block: (p: Pixels, x: Int, y: Int) -> Unit) {
        repeat(height) { y ->
            yield()
            repeat(width) { x ->
                block(this, x, y)
            }
        }
    }
}