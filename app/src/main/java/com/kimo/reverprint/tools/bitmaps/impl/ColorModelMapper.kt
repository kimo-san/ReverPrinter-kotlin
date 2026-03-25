package com.kimo.reverprint.tools.bitmaps.impl

import com.kimo.reverprint.tools.bitmaps.MutablePixels
import kotlin.math.roundToInt

suspend fun MutablePixels.toArgb8() {
    when (implementedModel) {

        is Monochrome -> {

            forEach { p, x, y ->
                val bwColor = p[x, y] * (Argb8.channelDepth - 1)

                p[x, y] = Argb8.colorOf(
                    a = Argb8.channelDepth - 1,
                    r = bwColor,
                    g = bwColor,
                    b = bwColor
                ).int
            }
        }

        is Grey4 -> {
            forEach { p, x, y ->

                val grayColor = (p.implementedModel
                    .colorOf(p[x, y])
                    .lum() * (Argb8.channelDepth - 1))
                    .roundToInt()

                p[x, y] = Argb8.colorOf(
                    a = Argb8.channelDepth - 1,
                    r = grayColor,
                    g = grayColor,
                    b = grayColor
                ).int
            }
        }

        else -> error("Unsupported conversation from ${implementedModel::class.simpleName} to Argb8")
    }
    // todo: add check
    implementedModel = Argb8
}

suspend fun MutablePixels.toGrey8bpp() {

    forEach { p, x, y ->
        val lum = p.implementedModel.colorOf(p[x, y]).lum() * (Grey8.channelDepth - 1)
        p[x, y] = lum.roundToInt()
    }

    require(pixelList.all { it in 0..<Grey8.channelDepth })
    implementedModel = Grey8
}

suspend fun MutablePixels.toMonochrome() {

    val avrLum = pixelList.map { implementedModel.colorOf(it).lum() }.average()
    forEach { p, x, y ->
        val lum = p.implementedModel.colorOf(p[x, y]).lum()
        p[x, y] = Monochrome.colorOf(if (lum > avrLum) 1 else 0).int
    }

    require(pixelList.all { it == 0 || it == 1 })
    implementedModel = Monochrome
}

suspend fun MutablePixels.toGrey4bpp() {

    forEach { p, x, y ->
        val lum = p.implementedModel.colorOf(p[x, y]).lum() * (Grey4.channelDepth - 1)
        p[x, y] = Grey4.colorOf(lum.roundToInt()).int
    }

    require(pixelList.all { it in 0..<Grey4.channelDepth })
    implementedModel = Grey4
}