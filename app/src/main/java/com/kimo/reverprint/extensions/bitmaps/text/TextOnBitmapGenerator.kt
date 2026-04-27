package com.kimo.reverprint.extensions.bitmaps.text

import com.kimo.reverprint.extensions.bitmaps.text.BitmapTextConfig
import com.kimo.reverprint.domain.images.ImagePixels

interface TextOnBitmapGenerator {
    suspend fun generatePixels(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels
}