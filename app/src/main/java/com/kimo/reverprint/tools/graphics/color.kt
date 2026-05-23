package com.kimo.reverprint.tools.graphics


@JvmInline
value class Color(val int: Int)

sealed interface ColorModel {

    val channelCount: Int
    val channelDepth: Int

    fun colorOf(intColor: Int): Color
    fun fromModel(sourceColor: Color, model: ColorModel): Color
    fun getChannelValue(src: Color, channel: Int): Int
    fun lumOf(color: Color): Float

}