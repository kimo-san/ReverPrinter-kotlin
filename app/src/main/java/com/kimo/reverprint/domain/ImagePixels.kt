package com.kimo.reverprint.domain

interface ImagePixels {
    val pixelList: List<Int>
    val width: Int
    val height: Int
    val model: ColorModel
    fun row(y: Int): IntArray = pixelList.slice(y * width ..< y * width + width).toIntArray()
}
