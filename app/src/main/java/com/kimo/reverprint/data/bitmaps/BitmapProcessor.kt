package com.kimo.reverprint.data.bitmaps

import android.graphics.Bitmap

suspend fun convertViewableImage(
    source: Bitmap,
    settings: BitmapProcessor.() -> Unit
): Pair<Bitmap, Pixels> {
    val processor = BitmapProcessorImpl()
    processor.setImage(source)
    processor.settings()
    val pixels = processor.processPixels()
    val viewable = processor.getViewable()
    return viewable to pixels
}

suspend fun BitmapProcessor.getViewable(): Bitmap {

    depthProColorChannel = DEFAULT_CHANNEL_DEPTH
    colorModel = Argb

    val pixels = processPixels()
    return Bitmap.createBitmap(
        pixels.arr,
        pixels.width,
        pixels.height,
        Bitmap.Config.ARGB_8888
    )
}

interface BitmapProcessor {

    fun setImage(bitmap: Bitmap)
    suspend fun processPixels(): Pixels

    var depthProColorChannel: Int
    var saturation: Float
    var dither: Boolean
    var width: Int
    var colorModel: ColorModel

}