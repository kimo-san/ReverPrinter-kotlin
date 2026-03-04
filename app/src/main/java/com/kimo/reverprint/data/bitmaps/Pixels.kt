package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap
import kotlin.math.min

class Pixels(
    val arr: IntArray,
    val width: Int,
    val height: Int
) {

    fun copy() = Pixels(arr.copyOf(), width, height)

    constructor(bitmap: Bitmap) : this(
        width = bitmap.width,
        height = bitmap.height,
        arr = IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    )

    private val maxIndex = width * height - 1
    fun getIndexForPixel(x: Int, y: Int) = min(
        y * width + min(x, width - 1),
        maxIndex
    )

    operator fun get(x: Int, y: Int): Int {
        return arr[getIndexForPixel(x, y)]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        arr[getIndexForPixel(x, y)] = value
    }
}