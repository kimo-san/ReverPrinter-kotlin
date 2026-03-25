package com.kimo.reverprint.androidData

import android.graphics.Bitmap
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.ImagePixels
import androidx.core.graphics.get
import com.kimo.reverprint.tools.bitmaps.ImmutablePixels


fun ImagePixels.asAndroidBitmap(): Bitmap {
    require(model == ColorModel.ARGB_8)
    return Bitmap.createBitmap(
        pixelList.toIntArray(),
        width, height,
        Bitmap.Config.ARGB_8888
    )
}


fun Bitmap.asImagePixels(): ImagePixels {
    val secureBitmap = copy(Bitmap.Config.ARGB_8888, false)
    val pixels = IntArray(width * height) { index ->
        val y = index / width
        val x = index % width
        secureBitmap[x, y]
    }
    return ImmutablePixels(
        intPixels = pixels,
        width = width,
        height = height,
        model = ColorModel.ARGB_8
    )
}

