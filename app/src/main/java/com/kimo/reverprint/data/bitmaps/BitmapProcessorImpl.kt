package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.roundToInt

class BitmapProcessorImpl : BitmapProcessor {

    private lateinit var pixels: Pixels
    private var settings: BitmapSettings? = null

    override fun setSettings(settings: BitmapSettings) {
        this.settings = settings
    }

    override fun setImage(bitmap: Bitmap) {
        val bm = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        pixels = Pixels(bm)
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
            pixels.model -> Unit
            is Grey4bpp -> {
                
                pixels.forEach { p, x, y ->
                    val lum =
                        p.model.colorOf(p[x, y]).lum() * (Grey4bpp.channelDepth - 1)
                    p[x, y] = Grey4bpp.colorOf(lum.roundToInt()).int
                }
                
                require(pixels.arr.all { it in 0..15 })
                pixels.model = Grey4bpp
            }

            is Monochrome -> {

                val avrLum = pixels.arr.mapIndexed { i, v ->
                    pixels.model.colorOf(pixels.arr[i]).lum()
                }.average()
                
                pixels.forEach { p, x, y ->
                    val lum = p.model.colorOf(p[x, y]).lum()
                    p[x, y] = Monochrome.colorOf(if (lum > avrLum) 1 else 0).int
                }

                require(pixels.arr.all { it in 0..1 })
                pixels.model = Monochrome
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

    suspend fun dither(): Unit = withContext(Dispatchers.Default) {

        /* orig color to nearest color */
        fun lookupTable(model: ColorModel): IntArray {

            val table = IntArray(256)
            val step = 255f / (model.channelDepth - 1)

            for (v in 0..255) {
                val index = (v / step).roundToInt()
                val quant = (index * step).roundToInt()
                table[v] = quant
            }

            return table
        }

        val lookup = lookupTable(pixels.model)
        fun findClosestColor(color: ColorOfModel): ColorOfModel =
            color.remappedValues { channel, value -> lookup[value] }

        fun applyDifference(
            color: ColorOfModel,
            errors: IntArray,
            factor: Float
        ): ColorOfModel =
            color.remappedValues { channel, _ ->
                (color[channel] + errors[channel] * factor).roundToInt()
            }

        pixels.forEach { p, x, y ->

            val oldPixel = p.model.colorOf(p[x, y])
            val newPixel = findClosestColor(oldPixel)

            val errorsOnChannels = IntArray(oldPixel.model.channelCount) { oldPixel[it] - newPixel[it] }
            p[x, y] = newPixel.int
            p[x + 1, y] = applyDifference(
                p.model.colorOf(p[x + 1, y]),
                errorsOnChannels,
                7f / 16
            ).int
            p[x - 1, y + 1] = applyDifference(
                p.model.colorOf(p[x - 1, y + 1]),
                errorsOnChannels,
                3f / 16
            ).int
            p[x, y + 1] = applyDifference(
                p.model.colorOf(p[x, y + 1]),
                errorsOnChannels,
                5f / 16
            ).int
            p[x + 1, y + 1] = applyDifference(
                p.model.colorOf(p[x + 1, y + 1]),
                errorsOnChannels,
                1f / 16
            ).int
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