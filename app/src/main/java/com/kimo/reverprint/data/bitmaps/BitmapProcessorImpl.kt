package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.properties.Delegates

class BitmapProcessorImpl : BitmapProcessor {

    private var pixels: Pixels = Pixels(intArrayOf(0), 1, 1)
    private var currentColorDepth = DEFAULT_CHANNEL_DEPTH
    private var currentColorModel: ColorModel = Argb

    override var depthProColorChannel = DEFAULT_CHANNEL_DEPTH
    override var saturation = 1f
    override var dither = false
    override var width by Delegates.notNull<Int>()
    override var colorModel: ColorModel = Argb

    override fun setImage(bitmap: Bitmap) {
        bitmap.copy(Bitmap.Config.ARGB_8888, true).apply {
            pixels = Pixels(this)
            this@BitmapProcessorImpl.width = width
            recycle()
        }
    }

    override suspend fun processPixels(): Pixels {

        if (this.width != pixels.width)
            resize(this.width, null)
        if (saturation != 1f)
            saturation(saturation)
        if (depthProColorChannel != currentColorDepth)
            depth(depthProColorChannel)
        if (dither)
            dither()
        if (colorModel != currentColorModel)
            changeColorModel(colorModel)

        return pixels.copy()
    }

    suspend fun changeColorModel(newModel: ColorModel) = withContext(Dispatchers.Default) {
        when (newModel) {

            is Greyscale -> {
                when (currentColorModel) {
                    is Greyscale -> {
                        return@withContext
                    }

                    else -> {
                        pixels.forEach { p, x, y ->
                            val argb = currentColorModel.colorOf(p[x, y]).lum()
                            p[x, y] = Greyscale.colorOf(
                                (argb * currentColorDepth).toInt()
                            ).int
                        }
                    }
                }
            }

            is Argb -> {
                when (currentColorModel) {
                    is Greyscale -> {
                        pixels.forEach { p, x, y ->
                            val lum = Greyscale
                                .colorOf(p[x, y])
                                .lum(Greyscale.W)
                                .times(currentColorDepth)
                                .toInt()
                            p[x, y] = Argb.colorOf(
                                a = 0,
                                r = (lum * Argb.luw(Argb.R)).toInt(),
                                g = (lum * Argb.luw(Argb.G)).toInt(),
                                b = (lum * Argb.luw(Argb.B)).toInt()
                            ).int
                        }
                    }

                    is Argb -> {
                        return@withContext
                    }

                    else -> error("Unsupported")
                }
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

        fun findClosestColor(color: ColorOfModel): Int =
            color.apply {
                channelValues.forEachIndexed { i, it ->
                    channelValues[i] = it / currentColorDepth * currentColorDepth
                }
            }.int

        pixels.forEach { p, x, y ->
            val oldPixel = p[x, y]
            val newPixel = findClosestColor(currentColorModel.colorOf(oldPixel))
            val error = oldPixel - newPixel
            p[x, y] = newPixel
            p[x + 1, y] += error * 7 / 16
            p[x - 1, y + 1] += error * 3 / 16
            p[x, y + 1] += error * 5 / 16
            p[x + 1, y + 1] += error * 1 / 16
        }
    }

    suspend fun depth(newDepth: Int): Unit = withContext(Dispatchers.Default) {
        pixels.forEach { p, x, y ->
            p[x, y] = currentColorModel.colorOf(p[x, y]).int
        }
        currentColorDepth = newDepth
    }

    suspend fun saturation(newSaturation: Float): Unit = withContext(Dispatchers.Default) {

        val inverted = 1f - newSaturation
        val matrix = CoefficientMatrix(currentColorModel.channelCount).also {
            repeat(it.colorChannels) { i ->
                val arr = FloatArray(it.colorChannels) { kch ->
                    currentColorModel.luw(kch) * inverted
                }
                arr[i] += newSaturation
                it[i] = arr
            }
        }

        pixels.forEach { p, x, y ->
            p[x, y] = currentColorModel.colorOf(p[x, y]).applyMatrix(matrix).int
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