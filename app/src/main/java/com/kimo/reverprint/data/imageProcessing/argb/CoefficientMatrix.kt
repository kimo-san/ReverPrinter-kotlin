package com.kimo.reverprint.data.imageProcessing.argb

/**
 *
 * Matrix for colors, which will be applied for each
 * color channel in form `pCh = Ch1*k1 + Ch2*k2 + Ch3*k3 + Ch4*k4`,
 * where `pCh` is a color channel we are applying for,
 * `ChN` is the actual value for `n` color channel
 * (where `n` is `R`, `G`, `B`, `A`) and `kn` is coefficient
 * for given color channel in the matrix.
 *
 * If you have no idea what is it, but you have somehow interest for it,
 * you can try to play with this
 * [source](https://kazzkiq.github.io/svg-color-filter/).
 *
 */
class CoefficientMatrix(val colorChannels: Int) {

    private val floatArray: FloatArray = initializeNormalMatrix()

    operator fun get(channel: Int, coefficientChannel: Int): Float =
        floatArray[channel * colorChannels + coefficientChannel]

    operator fun set(channel: Int, coefficients: FloatArray) {
        coefficients.copyInto(floatArray, channel * colorChannels)
    }


    /**
     *  Initialize color matrix without any filters.
     *
     *  The example of the output matrix:
     *  ```
     *  Channel     Coefficients
     *                 r g b a
     *    R           [1 0 0 0]
     *    G           [0 1 0 0]
     *    B           [0 0 1 0]
     *    A           [0 0 0 1]
     *  ```
     */
    private fun initializeNormalMatrix(): FloatArray = FloatArray(colorChannels * colorChannels) {
        val currentBlockIndex = it / colorChannels
        if (it % colorChannels == currentBlockIndex) 1f
        else 0f
    }
}