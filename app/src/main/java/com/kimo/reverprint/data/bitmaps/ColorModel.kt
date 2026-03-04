package com.kimo.reverprint.data.bitmaps

import kotlin.math.roundToInt

const val DEFAULT_CHANNEL_DEPTH = 256

interface ColorModel {

    val channelCount: Int
    val channelDepth: Int

    /**
     * Get concrete instance of color from int of this model
     */
    fun colorOf(
        intColor: Int
    ): ColorOfModel

    /**
     * Returns luminance weight of channel
     */
    fun luw(channel: Int): Float
}

abstract class ColorOfModel(
    val model: ColorModel
) {

    val channelCount: Int get() = model.channelCount
    val channelDepth: Int get() = model.channelDepth

    open val channelValues: IntArray = IntArray(channelCount)
    abstract val int: Int


    /**
     * Returns luminance of channel
     */
    fun lum(channel: Int): Float = channelValues[channel].toFloat() / channelDepth

    /**
     * Returns average luminance of all channels
     */
    fun lum(): Float = channelValues.average().toFloat() / channelCount / channelDepth

    fun applyMatrix(matrix: CoefficientMatrix): ColorOfModel {
        val oldRgba = channelValues.copyOf()
        for (channel in 0..<channelCount) {

            var newChannelValue = 0f
            for (coefficientChannel in 0..<channelCount) {
                val k = matrix[channel, coefficientChannel]
                val o = oldRgba[coefficientChannel]
                newChannelValue += o * k
            }

            channelValues[channel] = newChannelValue
                .roundToInt()
                .coerceIn(0, channelDepth)
        }
        return this
    }
}



