package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap

interface BitmapProcessor {

    fun setImage(bitmap: Bitmap)
    suspend fun processPixels(): Pixels
    fun setSettings(settings: BitmapSettings)

}