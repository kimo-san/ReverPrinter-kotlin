package com.kimo.reverprint.tools.graphics

import kotlin.math.max
import kotlin.math.min

/**
 * Basic representation of any operable bitmap
 */
interface Pixels {

    val width: Int
    val height: Int
    val colorModel: ColorModel
    val pixelList: List<Int>
    val config: BitmapConfig

    fun getCopy(): Pixels
    operator fun get(x: Int, y: Int): Color
    operator fun set(x: Int, y: Int, value: Color)
    suspend fun changeColorModel(targetModel: ColorModel)

}

interface BitmapCreator {
    suspend fun create(config: BitmapConfig): Pixels
    fun canApply(config: BitmapConfig): Boolean
}

data class BitmapConfig(
    val width: Int,
    val height: Int? = null,
    val colorModel: ColorModel,
    val storage: StorageType
)

enum class StorageType {
    RAM,
    NATIVE,
    FILE,
}

/**
 * Helper for implementation of own variants for bitmaps with different types of storages.
 */
@Suppress("NOTHING_TO_INLINE")
abstract class AbstractPixels: Pixels {

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Essentials to override

    abstract override val width: Int
    abstract override val height: Int
    abstract override var colorModel: ColorModel
        protected set
    protected abstract val storageType: StorageType
    protected abstract fun getIntColorForPixel(x: Int, y: Int): Int
    protected abstract fun setIntColorForPixel(x: Int, y: Int, value: Int)
    protected open fun handleReadCoordinateOutOfRange(x: Int, y: Int) { error("Coordinate ($x|$y) on the bitmap ${width}x$height is not present.") }
    protected open fun handleWriteCoordinateOutOfRange(x: Int, y: Int) { /* ignore by default */ }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Impl

    override val config: BitmapConfig
        get() = BitmapConfig(
            width = width,
            height = height,
            colorModel = colorModel,
            storage = storageType
        )

    final override fun get(x: Int, y: Int): Color {
        if (!isCoordinatePresent(x, y)) handleReadCoordinateOutOfRange(x, y)
        return Color(getIntColorForPixel(x, y))
    }

    final override operator fun set(x: Int, y: Int, value: Color) {
        if (!isCoordinatePresent(x, y)) handleWriteCoordinateOutOfRange(x, y)
        setIntColorForPixel(x, y, value.int)
    }

    final override val pixelList: List<Int>
        get() = object : AbstractList<Int>() {
            override val size: Int
                get() = width * height
            override fun get(index: Int): Int =
                getIntColorForPixel(index % width, index / width)
        }

    final override suspend fun changeColorModel(targetModel: ColorModel) {
        forEach { p, x, y ->
            this[x, y] = targetModel.fromModel(get(x, y), colorModel)
        }
        colorModel = targetModel
        ensureIsCorrect()
    }

    // - - - - - - - - - - - - - - - - - - - - - - -

    protected inline fun indexOf(x: Int, y: Int): Long =
        y.toLong() * width + x

    private inline fun isCoordinatePresent(x: Int, y: Int): Boolean =
        x in 0..<width && y in 0..<height

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