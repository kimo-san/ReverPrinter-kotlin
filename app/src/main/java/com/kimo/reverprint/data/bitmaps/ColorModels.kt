package com.kimo.reverprint.data.bitmaps

object Argb: ColorModel {

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
        ): this(android.graphics.Color.argb(a, r, g, b))

        override val model: ColorModel get () = Argb
        override fun get(channel: Int): Int = when (channel) {
            A -> android.graphics.Color.alpha(intColor)
            R -> android.graphics.Color.red(intColor)
            G -> android.graphics.Color.green(intColor)
            B -> android.graphics.Color.blue(intColor)
            else -> error("Channel $channel is not present for the model $model.")
        }
        override val int: Int get() = intColor

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = android.graphics.Color.luminance(int)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defRemapImpl(block)
            return ArgbColor(remap[A], remap[R], remap[G], remap[B])
        }
    }
}

object Grey4bpp: ColorModel {

    override val channelDepth: Int = 16
    override val channelCount: Int = 1
    const val W = 0

    override fun colorOf(
        intColor: Int
    ): ColorOfModel = GrayscaleColor(intColor)


    @JvmInline
    private value class GrayscaleColor(val intValue: Int): ColorOfModel {

        override val model: ColorModel get() = Grey4bpp
        override fun get(channel: Int) = intValue
        override val int: Int get() = intValue

        override fun lum(channel: Int): Float = get(channel).toFloat() / model.channelDepth
        override fun lum(): Float = lum(W)

        override fun remappedValues(block: (Int, Int) -> Int): ColorOfModel {
            val remap = defRemapImpl(block)
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
            val remap = defRemapImpl(block)
            return MonochromeColor(remap[W])
        }
    }
}

private fun ColorOfModel.defRemapImpl(block: (channel: Int, value: Int) -> Int): IntArray {
    val remap = IntArray(model.channelCount) { ch -> get(ch) }
    repeat(model.channelCount) { ch -> remap[ch] = block(ch, get(ch)) }
    return remap
}