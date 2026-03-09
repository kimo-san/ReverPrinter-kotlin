package com.kimo.reverprint.data.bitmaps

interface ColorModel {

    val channelCount: Int
    val channelDepth: Int

    /**
     * Returns concrete instance of color from int of this model
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
     * Returns luminance of all the color
     */
    fun lum(): Float

    fun remapped(block: Remappable<Int, Int>.(channel: Int, value: Int) -> Unit): ColorOfModel
    fun interface Remappable<K, T> {
        fun set(key: K, newValue: T)
    }
}



