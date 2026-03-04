package com.kimo.reverprint.data.bitmaps

open class Argb(
    override val channelDepth: Int
): ColorModel {

    override val channelCount: Int = 4
    companion object : Argb(DEFAULT_CHANNEL_DEPTH) {
        const val A = 0
        const val R = 1
        const val G = 2
        const val B = 3
    }

    override fun luw(channel: Int): Float =
        when(channel) {
            R -> 0.2126f
            G -> 0.7152f
            B -> 0.0722f
            else -> 1f
        }

    fun colorOf(
        a: Int, r: Int,
        g: Int, b: Int
    ): ColorOfModel = ArgbColor(a, r, g, b)

    override fun colorOf(intColor: Int): ColorOfModel = ArgbColor(intColor)

    private class ArgbColor(
        a: Int, r: Int,
        g: Int, b: Int
    ): ColorOfModel(this) {

        constructor(
            intColor: Int
        ): this(
            a = android.graphics.Color.alpha(intColor),
            r = android.graphics.Color.red(intColor),
            g = android.graphics.Color.green(intColor),
            b = android.graphics.Color.blue(intColor)
        )

        override val channelValues: IntArray = intArrayOf(a, r, g, b)
        override val int: Int
            get() = android.graphics.Color.argb(
                channelValues[A],
                channelValues[R],
                channelValues[G],
                channelValues[B],
            )
    }
}

open class Greyscale(
    override val channelDepth: Int
): ColorModel {

    override val channelCount: Int = 1
    companion object : Greyscale(DEFAULT_CHANNEL_DEPTH) {
        const val W = 0
    }
    override fun luw(channel: Int): Float = 1f

    override fun colorOf(
        intColor: Int
    ): ColorOfModel = object : ColorOfModel(this) {
        override val channelValues: IntArray = intArrayOf(intColor)
        override val int: Int
            get() = channelValues[W]
    }
}