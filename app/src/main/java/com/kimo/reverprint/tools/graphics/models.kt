package com.kimo.reverprint.tools.graphics

import kotlin.math.roundToInt

/*
 * Numbers in names of following objects indicate the color depth á channel
 */

object Argb8 : ColorModel {

    override val channelDepth: Int = 256
    override val channelCount: Int = 4
    const val A = 0
    const val R = 1
    const val G = 2
    const val B = 3

    override fun colorOf(intColor: Int): Color = Color(intColor and 0xFFFFFFF)
    fun colorOf(a: Int, r: Int, g: Int, b: Int): Color {
        val result = (a shl 24) or (r shl 16) or (g shl 8) or b
        return Color(result and 0xFFFFFFF)
    }

    override fun fromModel(sourceColor: Color, model: ColorModel): Color {
        return if (model == Argb8) sourceColor else if (model.channelCount == 1) {

            val luminance = model.lumOf(sourceColor)
            val valuePerChannel = (luminance * (channelDepth - 1)).roundToInt() and 0xff
            val a = 0xff
            val r = valuePerChannel
            val g = valuePerChannel
            val b = valuePerChannel
            val result = (a shl 24) or (r shl 16) or (g shl 8) or b
            Color(result)
        } else {
            error("Unsupported conversation from ${sourceColor::class.simpleName} to Argb8")
        }
    }

    override fun lumOf(color: Color): Float {
        val r = getChannelValue(color, R).toFloat() / 0xff
        val g = getChannelValue(color, G).toFloat() / 0xff
        val b = getChannelValue(color, B).toFloat() / 0xff
        val lum = (0.2126f * r) + (0.7152f * g) + (0.0722f * b)
        return lum
    }

    override fun getChannelValue(src: Color, channel: Int): Int =
        when (channel) {
            A -> src.int shr 24 and 0xff
            R -> src.int shr 16 and 0xff
            G -> src.int shr 8  and 0xff
            B -> src.int shr 0  and 0xff
            else -> error("Channel $channel is not present for the model Argb8.")
        }
}

object Grey8 : ColorModel {

    override val channelDepth: Int = 256
    override val channelCount: Int = 1

    override fun colorOf(
        intColor: Int
    ): Color = Color(intColor and 0xFF)


    override fun fromModel(sourceColor: Color, model: ColorModel): Color =
        Color((model.lumOf(sourceColor) * (channelDepth - 1)).roundToInt())

    override fun lumOf(color: Color): Float =
        color.int.toFloat() / channelDepth

    override fun getChannelValue(src: Color, channel: Int): Int {
        return src.int and 0xff
    }

}

object Grey4 : ColorModel {

    override val channelDepth: Int = 16
    override val channelCount: Int = 1

    override fun colorOf(
        intColor: Int
    ): Color = Color(intColor and 0xF)


    override fun fromModel(sourceColor: Color, model: ColorModel): Color =
        Color((model.lumOf(sourceColor) * (channelDepth - 1)).roundToInt())

    override fun lumOf(color: Color): Float =
        color.int.toFloat() / channelDepth

    override fun getChannelValue(src: Color, channel: Int): Int {
        return src.int and 0xf
    }
}

object Monochrome : ColorModel {

    override val channelDepth: Int = 1
    override val channelCount: Int = 1

    override fun colorOf(
        intColor: Int
    ): Color = Color(intColor and 0x1)

    override fun fromModel(sourceColor: Color, model: ColorModel): Color =
        Color(model.lumOf(sourceColor).roundToInt() and 0x1)

    override fun lumOf(color: Color): Float =
        (color.int).toFloat()

    override fun getChannelValue(src: Color, channel: Int): Int {
        return src.int
    }
}