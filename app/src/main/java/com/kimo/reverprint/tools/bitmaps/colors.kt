package com.kimo.reverprint.tools.bitmaps

interface ColorModel {

    val channelCount: Int
    val channelDepth: Int

    /**
     * Returns a concrete instance of color from int of this model
     */
    fun colorOf(
        intColor: Int
    ): ColorOfModel

}

interface ColorOfModel {

    val model: ColorModel
    val int: Int
    operator fun get(channel: Int): Int

    /**
     * Returns luminance of the given channel
     */
    fun lum(channel: Int): Float

    /**
     * Returns luminance of all channels in the color
     */
    fun lum(): Float

    /**
     * Remaps values of each color channel
     */
    fun remappedValues(block: (channel: Int, value: Int) -> Int): ColorOfModel
}



