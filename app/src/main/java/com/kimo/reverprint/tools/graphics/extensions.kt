package com.kimo.reverprint.tools.graphics

import com.kimo.reverprint.ForBitmapTests.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.math.roundToInt


suspend inline fun Pixels.forEach(block: (p: Pixels, x: Int, y: Int) -> Unit) {
    repeat(height) { y ->
        yield()
        repeat(width) { x ->
            block(this, x, y)
        }
    }
}

suspend inline fun Pixels.fullFill(color: Color) {
    forEach { p, x, y ->
        p[x, y] = color
    }
}

/**
 * The only function, which returns a new resized pixel array
 */
suspend fun Pixels.resizedCopy(
    setWidth: Int?,
    setHeight: Int?,
    creator: BitmapCreator
): Pixels = withContext(Dispatchers.Default) {

    val (originalWidth, originalHeight) = width to height
    val newWidth: Int
    val newHeight: Int

    when {
        (setWidth == null && setHeight != null) -> {
            newWidth = (originalWidth * setHeight / originalHeight).coerceAtLeast(1)
            newHeight = setHeight
        }

        (setWidth != null && setHeight == null) -> {
            newWidth = setWidth
            newHeight = (originalHeight * setWidth / originalWidth).coerceAtLeast(1)
        }

        else -> {
            newWidth = setWidth ?: return@withContext this@resizedCopy.getCopy()
            newHeight = setHeight ?: return@withContext this@resizedCopy.getCopy()
        }
    }

    val scaledBitmap = creator.create(
        BitmapConfig(
            newWidth,
            newHeight,
            colorModel,
            config.storage
        )
    )
    val (scaleX, scaleY) = (originalWidth.toFloat() / newWidth) to (originalHeight.toFloat() / newHeight)
    scaledBitmap.forEach { p, x, y ->
        p[x, y] = this@resizedCopy[
            (x * scaleX).toInt(),
            (y * scaleY).toInt()
        ]
    }

    scaledBitmap
}

suspend fun Pixels.changeColorModel(
    newModel: ColorModel,
    dither: Boolean
) = withContext(Dispatchers.Default) {

    val pixels = this@changeColorModel

    when (newModel) {
        pixels.colorModel -> Unit

        is Argb8 -> pixels.changeColorModel(Argb8)
        is Grey8 -> pixels.changeColorModel(Grey8)

        is Grey4 -> {
            if (dither && pixels.colorModel.channelDepth > Grey4.channelDepth) {
                pixels.changeColorModel(Grey8)
                fitToModelUsingDithering(pixels, Grey4)
            } else {
                pixels.changeColorModel(Grey4)
            }
        }

        is Monochrome -> {
            if (dither) {
                pixels.changeColorModel(Grey8)
                fitToModelUsingDithering(pixels, Monochrome)
            } else {
                pixels.changeColorModel(Monochrome)
            }
        }

        else -> error("Unsupported color model ${newModel::class.simpleName}")
    }
}

/**
 * Небезопасно вызывать, если пиксели для вставки не вмещаются в границы полотна.
 */
suspend fun Pixels.insertPixels(
    newPixels: Pixels,
    startX: Int,
    startY: Int,
) {
    newPixels.forEach { p, x, y ->
        this[x + startX, y + startY] = colorModel.fromModel(p[x, y], p.colorModel)
    }
}

suspend fun Pixels.fillRectangle(
    color: Color,
    colorModel: ColorModel,
    startX: Int,
    endX: Int,
    startY: Int,
    endY: Int
) {
    val color = this.colorModel.fromModel(color, colorModel)
    for (y in startY..endY) {
        yield()
        for (x in startX..endX)
            this[x, y] = color
    }
}

/**
 * Floyd Steinberg Algorithm
 */
private suspend fun fitToModelUsingDithering(
    pixels: Pixels,
    newModel: ColorModel
): Unit = withContext(Dispatchers.Default) {
    require(newModel.channelCount == 1) {
        "Dithering is currently applicable only for 1-channel colors. " +
        "Current model is ${pixels.colorModel.name}"
    }

    val maxLevel = max(newModel.channelDepth - 1, 1)

    val step = max(pixels.colorModel.channelDepth - 1, 1) / maxLevel
    pixels.forEach { p, x, y ->

        val oldPixelValue = p[x, y].int
        val newPixelValue = (oldPixelValue * 1f / step).roundToInt() * step
        val error = oldPixelValue - newPixelValue

        p[x, y] = p.colorModel.colorOf(newPixelValue)
        p[x + 1, y] = p.colorModel.colorOf((p[x + 1, y].int + (error * 7f / 16)).toInt())
        p[x, y + 1] = p.colorModel.colorOf((p[x, y + 1].int + (error * 5f / 16)).toInt())
        p[x - 1, y + 1] = p.colorModel.colorOf((p[x - 1, y + 1].int + (error * 3f / 16)).toInt())
        p[x + 1, y + 1] = p.colorModel.colorOf((p[x + 1, y + 1].int + (error * 1f / 16)).toInt())
    }

    pixels.changeColorModel(newModel)
}