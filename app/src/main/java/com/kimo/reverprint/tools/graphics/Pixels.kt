package com.kimo.reverprint.tools.graphics

import com.kimo.reverprint.rowsAreSame
import com.kimo.reverprint.name
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.math.min

fun Pixels(
    width: Int,
    height: Int,
    colorModel: ColorModel,
    pixelsArray: IntArray
): Pixels = PixelsImpl(width, height, colorModel, pixelsArray)

fun Pixels(
    width: Int,
    height: Int,
    colorModel: ColorModel,
    defaultColor: Color
): Pixels = PixelsImpl(
    width, height, colorModel,
    IntArray(height * width) { defaultColor.int }
)

interface Pixels {

    val width: Int
    val height: Int
    val colorModel: ColorModel
    val pixelList: List<Int>

    fun getCopy(): Pixels
    suspend fun forEach(block: (p: Pixels, x: Int, y: Int) -> Unit)
    operator fun get(x: Int, y: Int): Color
    operator fun set(x: Int, y: Int, value: Color)
    suspend fun changeColorModel(targetModel: ColorModel)

}


private class PixelsImpl(
    override val width: Int,
    override val height: Int,
    override var colorModel: ColorModel,
    private val pixelsArray: IntArray = IntArray(width * height)
) : Pixels {

    override suspend fun changeColorModel(targetModel: ColorModel) {
        forEach { p, x, y ->
            this[x, y] = targetModel.fromModel(get(x, y), colorModel)
        }
        colorModel = targetModel
        ensureIsCorrect()
    }

    override val pixelList: List<Int>
        get() = pixelsArray.asList()

    override fun getCopy(): PixelsImpl {
        println("PixelsImpl.getCopy ${colorModel.name} All were same: " + rowsAreSame(50,51,100))
        val result = PixelsImpl(
            pixelsArray = pixelsArray.copyOf(),
            width = width,
            height = height,
            colorModel = colorModel
        )
        println("PixelsImpl.getCopy ${result.colorModel.name} All are same: " + result.rowsAreSame(50,51,100))
        return result
    }

    private val maxIndex = width * height - 1
    private fun getIndexForPixel(x: Int, y: Int) = min(
        y * width + min(x, width - 1),
        maxIndex
    )

    override fun get(x: Int, y: Int): Color {
        return Color(pixelsArray[getIndexForPixel(x, y)])
    }

    override operator fun set(x: Int, y: Int, value: Color) {
        pixelsArray[getIndexForPixel(x, y)] = value.int
    }

    override suspend fun forEach(block: (p: Pixels, x: Int, y: Int) -> Unit) {
        repeat(height) { y ->
            yield()
            repeat(width) { x ->
                block(this, x, y)
            }
        }
    }

    private fun ensureIsCorrect() {
        val allowedRange = 0..<(colorModel.channelDepth.coerceAtLeast(2))
        var maxVal = Int.MIN_VALUE
        var minVal = Int.MAX_VALUE
        pixelsArray.forEach { rowValue ->

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