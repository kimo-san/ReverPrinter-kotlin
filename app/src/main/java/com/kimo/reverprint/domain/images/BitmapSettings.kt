package com.kimo.reverprint.domain.images

data class BitmapSettings(
    val dither: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val colorModel: ColorModel? = null
)