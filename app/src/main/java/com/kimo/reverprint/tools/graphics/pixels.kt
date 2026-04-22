package com.kimo.reverprint.tools.graphics

import kotlin.math.max
import kotlin.math.min

interface BitmapCreator {

    suspend fun create(
        width: Int,
        height: Int,
        colorModel: ColorModel
    ): Pixels

    suspend fun createExtendable(
        width: Int,
        colorModel: ColorModel
    ): Pixels
}

/**
 * Basic representation of any operable bitmap
 */
interface Pixels {

    val width: Int
    val height: Int
    val colorModel: ColorModel
    val pixelList: List<Int>

    fun getCopy(): Pixels
    operator fun get(x: Int, y: Int): Color
    operator fun set(x: Int, y: Int, value: Color)
    suspend fun changeColorModel(targetModel: ColorModel)

}

/**
 * For larger images
 */
interface CloseablePixels: Pixels, AutoCloseable

abstract class AbstractPixels: Pixels {

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Basic essentials to override

    abstract override val width: Int
    abstract override val height: Int
    abstract override var colorModel: ColorModel
        protected set
    protected abstract fun getIntColorForPixel(index: Int): Int
    protected abstract fun setIntColorForPixel(index: Int, value: Int)

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Impl

    @Suppress("NOTHING_TO_INLINE")
    private inline fun getIndex(x: Int, y: Int) = y * width + x

    final override fun get(x: Int, y: Int): Color {
        return Color(getIntColorForPixel(getIndex(x, y)))
    }

    final override operator fun set(x: Int, y: Int, value: Color) {
        setIntColorForPixel(getIndex(x, y), value.int)
    }

    final override val pixelList: List<Int>
        get() = object : AbstractList<Int>() {
            override val size: Int
                get() = width * height
            override fun get(index: Int): Int =
                getIntColorForPixel(index)
        }

    final override suspend fun changeColorModel(targetModel: ColorModel) {
        forEach { p, x, y ->
            this[x, y] = targetModel.fromModel(get(x, y), colorModel)
        }
        colorModel = targetModel
        ensureIsCorrect()
    }

    // - - - - - - - - - - - - - - - - - - - - - - -

    private fun ensureIsCorrect() {
        val allowedRange = 0..<(colorModel.channelDepth.coerceAtLeast(2))
        var maxVal = Int.MIN_VALUE
        var minVal = Int.MAX_VALUE
        pixelList.forEach { rowValue ->
            repeat(colorModel.channelCount) { channel ->
                val channelValue = colorModel.getChannelValue(Color(rowValue), channel)
                maxVal = max(channelValue, maxVal)
                minVal = min(channelValue, minVal)

                if (channelValue !in allowedRange)
                    println("Out of range: $channelValue...")
            }
        }
        require(0 in allowedRange && maxVal in allowedRange) {
            "Debug info : actual color model is $colorModel, min $minVal, max $maxVal"
        }
    }
}