package com.kimo.reverprint.domain

typealias Position = Int
typealias PixelValue = Int
typealias PixelValues = IntArray

interface ImagePixels {
    val pixelList: List<PixelValue>
    val width: Int
    val height: Int
    val model: ColorModel
    fun row(y: Position): PixelValues
}
