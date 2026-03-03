package com.kimo.reverprint.data.imageProcessing.argb

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import com.kimo.reverprint.data.imageProcessing.BitmapProcessor
import com.kimo.reverprint.data.imageProcessing.argb.ArgbColor.Companion.A
import com.kimo.reverprint.data.imageProcessing.argb.ArgbColor.Companion.B
import com.kimo.reverprint.data.imageProcessing.argb.ArgbColor.Companion.G
import com.kimo.reverprint.data.imageProcessing.argb.ArgbColor.Companion.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class ArgbBitmapProcessor : BitmapProcessor {

    private lateinit var pixels: Pixels
    private var currentColorDepth = ArgbColor.COLOR_DEPTH


    override suspend fun setBitmap(bp: Bitmap) {
        val copy = bp.copy(Bitmap.Config.ARGB_8888, true)
        pixels = Pixels(copy)
        copy.recycle()
    }

    override suspend fun getBitmap(): Bitmap {
        depth(ArgbColor.COLOR_DEPTH)
        return Bitmap.createBitmap(
            pixels.arr,
            pixels.width,
            pixels.height,
            Bitmap.Config.ARGB_8888
        )
    }

    override suspend fun close() {
        pixels = Pixels(intArrayOf(0), 1, 1)
    }

    // FIXME
    override suspend fun resize(width: Int): Unit = withContext(Dispatchers.Default) {

        val (newWidth, newHeight) = width to (pixels.height * width / pixels.width).coerceAtLeast(1)

        val scaled = Pixels(IntArray(newWidth * newHeight), newWidth, newHeight)

//        val scaleY = pixels.height.toFloat() / newHeight
//        val scaleX = pixels.width.toFloat() / newWidth
        scaled.forEach { p, x, y ->
//            val x = (x * scaleX).toInt().coerceIn(0, pixels.width - 1)
//            val y = (y * scaleY).toInt().coerceIn(0, pixels.height - 1)
//            p[x, y] = pixels[x, y]
            p[x, y] = ArgbColor.COLOR_DEPTH.let { ArgbColor(it-1, 0, it-1, it-1) }.int
        }

        pixels = scaled
    }

    override suspend fun dither(): Unit = withContext(Dispatchers.Default) {

        fun findClosestColor(color: Int): Int =
            ArgbColor.Companion.fromInt(color).apply {
                r = r / currentColorDepth * currentColorDepth
                g = g / currentColorDepth * currentColorDepth
                b = b / currentColorDepth * currentColorDepth
                a = a / currentColorDepth * currentColorDepth
            }.int

        pixels.forEach { p, x, y ->

            val oldPixel = p[x, y]
            val newPixel = findClosestColor(oldPixel)
            val error = oldPixel - newPixel

            p[x, y] = newPixel
            p[x + 1, y] += error * 7 / 16
            p[x - 1, y + 1] += error * 3 / 16
            p[x, y + 1] += error * 5 / 16
            p[x + 1, y + 1] += error * 1 / 16

        }
    }

    override suspend fun depth(
        colorsPerPixel: Int
    ): Unit = withContext(Dispatchers.Default) {
        fun coerceColor(value: Int): Int {
            val step = currentColorDepth.toFloat() / colorsPerPixel
            return ((value / step).toInt() * step).toInt()
        }
        pixels.forEach { p, x, y ->
            p[x, y] = ArgbColor.Companion.fromInt(p[x, y])
                .apply {
                    r = coerceColor(r)
                    g = coerceColor(g)
                    b = coerceColor(b)
                    a = coerceColor(a)
                }.int
        }
        currentColorDepth = colorsPerPixel
    }

    override suspend fun saturation(
        @FloatRange(0.0, 1.0) s: Float
    ): Unit = withContext(Dispatchers.Default) {

        fun lum(channel: Int): Float = when(channel) {
            R -> 0.2126f
            G -> 0.7152f
            B -> 0.0722f
            else -> error("Does not exist")
        }

        val inv = 1f - s
        val matrix = CoefficientMatrix(ArgbColor.CHANNELS).also {
            it[R] = floatArrayOf(lum(R) * inv + s, lum(G) * inv, lum(B) * inv, 0f)
            it[G] = floatArrayOf(lum(R) * inv, lum(G) * inv + s, lum(B) * inv, 0f)
            it[B] = floatArrayOf(lum(R) * inv, lum(G) * inv, lum(B) * inv + s, 0f)
            it[A] = floatArrayOf(0f,           0f,               0f,           1f)
        }

        pixels.forEach { p, x, y ->
            p[x, y] = ArgbColor.fromInt(p[x, y])
                .applyMatrix(matrix)
                .int
        }
    }

    private suspend fun Pixels.forEach(block: (Pixels, Int, Int) -> Unit) {
        repeat(height) { y ->
            yield()
            repeat(width) { x ->
                block(this, x, y)
            }
        }
    }
}