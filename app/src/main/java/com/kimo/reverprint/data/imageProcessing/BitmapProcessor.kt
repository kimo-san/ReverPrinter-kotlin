package com.kimo.reverprint.data.imageProcessing

import android.graphics.Bitmap
import androidx.annotation.FloatRange

interface BitmapProcessor {

    suspend fun process(
        source: Bitmap,
        block: suspend BitmapProcessor.() -> Unit
    ): Bitmap = this
        .also { setBitmap(source) }
        .also { block() }
        .getBitmap()
        .also { close() }

    suspend fun setBitmap(bp: Bitmap)
    suspend fun getBitmap(): Bitmap
    suspend fun close()

    suspend fun depth(colorsPerPixel: Int)
    suspend fun saturation(@FloatRange(0.0, 1.0) s: Float)
    suspend fun dither()
    suspend fun resize(width: Int)

}