package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.StorageType
import kotlin.math.min

class RamBitmap(
    override val width: Int,
    override val height: Int,
    override var colorModel: ColorModel,
    private val pixelsArray: IntArray
) : AbstractPixels() {

    override val storageType: StorageType = StorageType.RAM

    @Suppress("NOTHING_TO_INLINE")
    private inline fun fit(index: Int): Int {
        return min(index, pixelsArray.size - 1)
    }

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        return pixelsArray[fit(indexOf(x, y).toInt())]
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        pixelsArray[fit(indexOf(x, y).toInt())] = value
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