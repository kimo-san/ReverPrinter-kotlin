package com.kimo.reverprint.androidData

import android.graphics.Bitmap
import androidx.core.graphics.get
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.ImagePixels

fun ImagePixels.toAndroidBitmap(): Bitmap {
    require(model == ColorModel.ARGB_8) { "Pixel should be in ARGB_8888 format" }

    val result =  Bitmap.createBitmap(
        pixelList.toIntArray(),
        width, height,
        Bitmap.Config.ARGB_8888
    )

    return result
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