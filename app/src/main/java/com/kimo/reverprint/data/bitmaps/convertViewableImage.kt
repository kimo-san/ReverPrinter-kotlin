package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap

suspend fun convertViewableImage(
    source: Bitmap,
    settings: BitmapSettings
): Pair<Bitmap, Pixels> {
    val processor = BitmapProcessorImpl()
    processor.setImage(source)
    processor.setSettings(settings)
    val pixels = processor.processPixels()
    val viewable = processor.getViewable()
    return viewable to pixels
}

suspend fun BitmapProcessor.getViewable(): Bitmap {
    setSettings(BitmapSettings(colorModel = Argb))
    val pixels = processPixels()
    return Bitmap.createBitmap(
        pixels.arr,
        pixels.width,
        pixels.height,
        Bitmap.Config.ARGB_8888
    )
}