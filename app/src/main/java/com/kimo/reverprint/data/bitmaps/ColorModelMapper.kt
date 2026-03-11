package com.kimo.reverprint.data.bitmaps

import kotlin.math.roundToInt

suspend fun Pixels.toGrey8bpp() {

    forEach { p, x, y ->
        val lum = p.model.colorOf(p[x, y]).lum() * (Grey8bpp.channelDepth - 1)
        p[x, y] = lum.roundToInt()
    }

    require(arr.all { it in 0..<Grey8bpp.channelDepth })
    model = Grey8bpp
}

suspend fun Pixels.toMonochrome() {

    val avrLum = arr.indices.map { model.colorOf(arr[it]).lum() }.average()
    forEach { p, x, y ->
        val lum = p.model.colorOf(p[x, y]).lum()
        p[x, y] = Monochrome.colorOf(if (lum > avrLum) 1 else 0).int
    }

    require(arr.all { it == 0 || it == 1 })
    model = Monochrome
}

suspend fun Pixels.toGrey4bpp() {

    forEach { p, x, y ->
        val lum = p.model.colorOf(p[x, y]).lum() * (Grey4bpp.channelDepth - 1)
        p[x, y] = Grey4bpp.colorOf(lum.roundToInt()).int
    }

    require(arr.all { it in 0..<Grey4bpp.channelDepth })
    model = Grey4bpp
}