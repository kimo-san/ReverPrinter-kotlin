package com.kimo.reverprint.tools.bitmaps.impl

// todo: get independent from android.graphics.Color
import android.graphics.Color
import com.kimo.reverprint.tools.bitmaps.ColorModel
import com.kimo.reverprint.tools.bitmaps.ColorOfModel

/*
 * Numbers in names of following objects indicate the color depth
 */

object Argb8: ColorModel {

    override val channelDepth: Int = 256
    override val channelCount: Int = 4
    const val A = 0
    const val R = 1
    const val G = 2
    const val B = 3

    fun colorOf(a: Int, r: Int, g: Int, b: Int): ColorOfModel = ArgbColor(a, r, g, b)

    override fun colorOf(intColor: Int): ColorOfModel = ArgbColor(intColor)

    @JvmInline
    private value class ArgbColor(val intColor: Int): ColorOfModel {

        constructor(
            a: Int, r: Int,
            g: Int, b: Int,
        ): this(Color.argb(a, r, g, b))

        override val model: ColorModel get () = Argb8
        override fun get(channel: Int): Int = when (channel) {
            A -> Color.alpha(intColor)
            R -> Color.red(intColor)
            G -> Color.green(intColor)
            B -> Color.blue(intColor)
            else -> error("Channel $channel is not present for the model $model.")
        }
        override val int: Int get() = intColor

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = Color.luminance(int)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defaultRemapImpl(block)
            return ArgbColor(remap[A], remap[R], remap[G], remap[B])
        }
    }
}

object Grey8: ColorModel {

    override val channelDepth: Int = 256
    override val channelCount: Int = 1
    const val W = 0

    override fun colorOf(intColor: Int): ColorOfModel = Color(intColor)

    @JvmInline
    private value class Color(val intValue: Int): ColorOfModel {

        override val model: ColorModel get() = Grey8
        override fun get(channel: Int) = intValue
        override val int: Int get() = intValue

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = lum(W)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defaultRemapImpl(block)
            return Color(remap[W])
        }
    }
}

object Grey4: ColorModel {

    override val channelDepth: Int = 16
    override val channelCount: Int = 1
    const val W = 0

    override fun colorOf(
        intColor: Int
    ): ColorOfModel = GrayscaleColor(intColor)


    @JvmInline
    private value class GrayscaleColor(val intValue: Int): ColorOfModel {

        override val model: ColorModel get() = Grey4
        override fun get(channel: Int) = intValue
        override val int: Int get() = intValue

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = lum(W)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defaultRemapImpl(block)
            return GrayscaleColor(remap[W])
        }
    }
}

object Monochrome: ColorModel {

    override val channelDepth: Int = 1
    override val channelCount: Int = 1
    const val W = 0

    override fun colorOf(
        intColor: Int
    ): ColorOfModel = MonochromeColor(intColor)

    @JvmInline
    private value class MonochromeColor(val intValue: Int): ColorOfModel {

        override val model: ColorModel get() = Monochrome
        override fun get(channel: Int) = intValue
        override val int: Int get() = intValue

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = lum(W)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defaultRemapImpl(block)
            return MonochromeColor(remap[W])
        }
    }
}

private fun ColorOfModel.defaultRemapImpl(block: (channel: Int, value: Int) -> Int): IntArray {
    val remap = IntArray(model.channelCount) { ch -> get(ch) }
    repeat(model.channelCount) { ch -> remap[ch] = block(ch, get(ch)) }
    return remap
}