package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import kotlin.math.min

class RamBitmapCreator: MonomodeFabric {
    fun create(
        width: Int,
        height: Int,
        colorModel: ColorModel
    ): Pixels =  RamBitmap(width, height, colorModel, IntArray(width * height))
}

private class RamBitmap(
    override val width: Int,
    override val height: Int,
    override var colorModel: ColorModel,
    private val pixelsArray: IntArray
) : AbstractPixels() {

    @Suppress("NOTHING_TO_INLINE")
    private inline fun fit(index: Int): Int {
        return min(index, pixelsArray.size - 1)
    }

    override fun getIntColorForPixel(index: Int): Int {
        return pixelsArray[fit(index)]
    }

    override fun setIntColorForPixel(index: Int, value: Int) {
        pixelsArray[fit(index)] = value
    }

    override fun getCopy(): RamBitmap {
        val result = RamBitmap(
            pixelsArray = pixelsArray.copyOf(),
            width = width,
            height = height,
            colorModel = colorModel
        )
        return result
    }
}