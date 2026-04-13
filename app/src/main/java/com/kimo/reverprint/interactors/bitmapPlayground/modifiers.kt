package com.kimo.reverprint.interactors.bitmapPlayground

import com.kimo.reverprint.name
import com.kimo.reverprint.rowsAreSame
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The only function, which returns a new resized pixel array
 */
suspend fun Pixels.resized(
    setWidth: Int?,
    setHeight: Int?
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
            newWidth = setWidth ?: return@withContext this@resized.getCopy()
            newHeight = setHeight ?: return@withContext this@resized.getCopy()
        }
    }


    println("Pixels.resized ${colorModel.name} All were same: " + rowsAreSame(50,51,100))
    val scaledBitmap = Pixels(
        pixelsArray = IntArray(newWidth * newHeight),
        width = newWidth,
        height = newHeight,
        colorModel = colorModel
    )
    val (scaleX, scaleY) = (originalWidth.toFloat() / newWidth) to (originalHeight.toFloat() / newHeight)
    scaledBitmap.forEach { p, x, y ->
        p[x, y] = this@resized[
            (x * scaleX).toInt(),
            (y * scaleY).toInt()
        ]
    }
    println("Pixels.resized ${scaledBitmap.colorModel.name} All are same: " + scaledBitmap.rowsAreSame(50,51,100))

    scaledBitmap
}

suspend fun changeColorModel(
    newModel: ColorModel,
    pixels: Pixels,
    dither: Boolean
) = withContext(Dispatchers.Default) {

    println("changeColorModel ${pixels.colorModel.name} All were same: " + pixels.rowsAreSame(50,51,100))

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

    println("changeColorModel ${pixels.colorModel.name} All are same: " + pixels.rowsAreSame(50,51,100))
}

suspend fun Pixels.insertPixels(
    newPixels: Pixels,
    startX: Int,
    startY: Int
) {
    newPixels.forEach { p, x, y ->
        val placedX = x + startX
        val placedY = y + startY
        if (placedY < this.height && placedX < this.width)
            this[placedX, placedY] = this.colorModel.fromModel(p[x, y], p.colorModel)
    }
}

/**
 * Algorithm by Floyd-Steinberg
 */
private suspend fun fitToModelUsingDithering(
    pixels: Pixels,
    newModel: ColorModel
): Unit = withContext(Dispatchers.Default) {
    require(newModel.channelCount == 1) {
        "Dithering is currently applicable only for 1-channel colors. Current model is ${pixels.colorModel.name}"
    }

    val maxLevel = max(newModel.channelDepth - 1, 1)

    val step = pixels.colorModel.channelDepth / maxLevel
    pixels.forEach { p, x, y ->

        val oldPixelValue = p[x, y].int
        val newPixelValue = (oldPixelValue * 1f / step).roundToInt() * step
        val error = oldPixelValue - newPixelValue

        p[x    , y    ] = p.colorModel.colorOf(newPixelValue)
        p[x + 1, y    ] = p.colorModel.colorOf((p[x + 1, y    ].int + error * 7f / 16).toInt())
        p[x    , y + 1] = p.colorModel.colorOf((p[x    , y + 1].int + error * 5f / 16).toInt())
        p[x - 1, y + 1] = p.colorModel.colorOf((p[x - 1, y + 1].int + error * 3f / 16).toInt())
        p[x + 1, y + 1] = p.colorModel.colorOf((p[x + 1, y + 1].int + error * 1f / 16).toInt())
    }

    pixels.changeColorModel(newModel)
}