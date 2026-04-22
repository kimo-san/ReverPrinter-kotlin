package com.kimo.reverprint.android

import android.graphics.Bitmap
import androidx.core.graphics.get
import com.kimo.reverprint.domain.images.ColorModel
import com.kimo.reverprint.domain.images.ImagePixels
import androidx.core.graphics.createBitmap

fun ImagePixels.toAndroidBitmap(): Bitmap {
    require(model == ColorModel.ARGB_8) { "Pixels should be in ARGB_8888 format" }

    val bitmap = createBitmap(width, height)
    for (y in 0 until height) {
        val row = row(y)
        bitmap.setPixels(
            row,
            0, width,
            0, y,
            width, 1
        )
    }

    return bitmap
}


fun Bitmap.toImagePixels(): ImagePixels {

    val secureBitmap = copy(Bitmap.Config.ARGB_8888, false)
    val pixels = IntArray(width * height) { index ->
        val y = index / width
        val x = index % width
        secureBitmap[x, y]
    }

    val result = object : ImagePixels {
        override val pixelList: List<Int>
        get() = pixels.asList()
        override val width: Int
        get() = this@toImagePixels.width
        override val height: Int
        get() = this@toImagePixels.height
        override val model: ColorModel
        get() = ColorModel.ARGB_8
    }

    return result
}