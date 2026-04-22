package com.kimo.reverprint

import com.kimo.reverprint.data.pixels.RamBitmapCreator
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.extensions.bitmaps.asDomainImmutable
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.forEach
import kotlinx.coroutines.runBlocking

fun createGradientBitmap(height: Int, width: Int): ImagePixels = runBlocking {

    val cr = RamBitmapCreator()
    val pixels = cr.create(
        height = height,
        width = width,
        colorModel = Argb8
    )

    pixels.forEach { p, x, y ->
        val luminance = x / p.width.toFloat()
        val grey = (luminance * Argb8.channelDepth).toInt()
        p[x, y] = Argb8.colorOf(0, grey, grey, grey)
    }

    pixels.asDomainImmutable()
}

fun createChessBitmap(height: Int, width: Int, gradations: Int): ImagePixels = runBlocking {

    val cr = RamBitmapCreator()
    val pixels = cr.create(
        height = height,
        width = width,
        colorModel = Argb8
    )

    pixels.forEach { p, x, y ->
        val luminance = (x + y) % gradations / gradations.toFloat()
        val grey = (luminance * Argb8.channelDepth).toInt()
        p[x, y] = Argb8.colorOf(0, grey, grey, grey)
    }

    pixels.asDomainImmutable()
}

fun present(arr: List<*>, breakAvery: Int) {
    arr.forEachIndexed { index, i ->
        if (index % breakAvery == 0) print('\n')
        print("$i ")
    }
    print('\n')
}

fun present(arr: IntArray, breakAvery: Int) {
    arr.map { it.toHexString(HexFormat { upperCase = true }) }
        .also { present(it, breakAvery) }
}


val ColorModel.name get() = this::class.simpleName

/*
fun Pixels.rowsAreSame(vararg rows: Int): Boolean {
    return runCatching {
        rows.map { pixelList.slice(width * it..width * it + width) }
            .flatMap { it.toSet() }
            .toSet()
            .size == 1
    }.getOrElse { false }
}
*/