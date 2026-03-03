package com.kimo.reverprint.data.imageProcessing.argb

import android.graphics.Color
import com.kimo.reverprint.data.imageProcessing.argb.CoefficientMatrix
import kotlin.math.roundToInt

class ArgbColor(
    a: Int, r: Int,
    g: Int, b: Int,
) {

    private val rgba = arrayOf(r, g, b, a)

    fun applyMatrix(matrix: CoefficientMatrix): ArgbColor {

        require(matrix.colorChannels == CHANNELS)
        val oldRgba = rgba.copyOf()

        for (channel in 0..<CHANNELS) {
            var channelValue = 0f
            for (coefficientChannel in 0..<CHANNELS) {
                val k = matrix[channel, coefficientChannel]
                val o = oldRgba[coefficientChannel]
                channelValue += o * k
            }
            rgba[channel] = channelValue.roundToInt()
        }
        return this
    }

    var r: Int
        get() = rgba[R]
        set(value) { rgba[R] = value }
    var g: Int
        get() = rgba[G]
        set(value) { rgba[G] = value }
    var b: Int
        get() = rgba[B]
        set(value) { rgba[B] = value }
    var a: Int
        get() = rgba[A]
        set(value) { rgba[A] = value }

    val int get() = Color.argb(a, r, g, b)

    companion object {

        const val COLOR_DEPTH = 256
        const val CHANNELS = 4
        const val R = 0
        const val G = 1
        const val B = 2
        const val A = 3

        fun fromInt(intColor: Int) =
            ArgbColor(
                a = Color.alpha(intColor),
                r = Color.red(intColor),
                g = Color.green(intColor),
                b = Color.blue(intColor)
            )
    }
}