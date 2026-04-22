package com.kimo.reverprint.domain.images

interface TextOnBitmapGenerator {
    suspend fun generatePixels(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels
}