package com.kimo.reverprint.tools.bitmaps

import com.kimo.reverprint.domain.BitmapSettings
import com.kimo.reverprint.domain.ImagePixels

interface BitmapProcessor {
    val image: ImagePixels
    val settings: BitmapSettings
    suspend fun processPixels(): MutablePixels
}