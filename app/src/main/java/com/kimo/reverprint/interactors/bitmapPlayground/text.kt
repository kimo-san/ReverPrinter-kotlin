package com.kimo.reverprint.interactors.bitmapPlayground

import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import kotlin.collections.plusAssign

suspend fun generateImageWithText(
    text: String,
    config: BitmapTextConfig
) = TextOnBitmapGeneratorImpl()
    .placeTextOnBitmap(text, config)

interface TextOnBitmapGenerator {

    suspend fun placeTextOnBitmap(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels
}

data class BitmapTextConfig (
    val width: Int,
    val letterHeight: Int,
    val letterSpacing: Int,
    val lineSpacing: Int,
    val colors: Colors,
    val font: Font,
    val dither: Boolean
) {
    class Colors (
        val model: ColorModel,
        val foreground: Color,
        val background: Color,
    )
}

data class FontParameters(
    val size: Int,
    val foreground: Color,
    val background: Color
)

interface Font {
    fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph
}

/**
 * LtR/UtB coordinates
 */
@JvmInline
value class Glyph (val bitmap: Pixels)

private class TextOnBitmapGeneratorImpl : TextOnBitmapGenerator {
    override suspend fun placeTextOnBitmap(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels {

        val imageRows = mutableListOf<Pixels>()
        val colorModel = config.colors.model

        var currentRowStart = 0
        var currentRow = 0

        fun presentRow() =
            imageRows.getOrElse(currentRow) {
                do {
                    imageRows += Pixels(
                        width = config.width,
                        height = config.letterHeight,
                        colorModel = colorModel,
                        defaultColor = config.colors.background,
                    )
                } while (imageRows.lastIndex < currentRow)
                imageRows[currentRow]
            }

        text.forEach { char ->
            when (char) {
                '\n' -> {
                    currentRow++
                    currentRowStart = 0
                }
                else -> {

                    val toInsert = config.font
                        .getBitmapOfChar(
                            char,
                            FontParameters(
                                size = config.letterHeight,
                                foreground = config.colors.foreground,
                                background = config.colors.background
                            )
                        )

                    if (currentRowStart + toInsert.bitmap.width > presentRow().width) {
                        currentRow++
                        currentRowStart = 0
                    }

                    presentRow().insertPixels(
                        newPixels = toInsert.bitmap,
                        startX = currentRowStart,
                        startY = 0
                    )

                    currentRowStart += toInsert.bitmap.width +
                            config.letterSpacing
                }
            }
        }

        val finalHeight = (config.letterHeight + config.lineSpacing) * imageRows.size.coerceAtLeast(1)
        val result = Pixels(
            width = config.width,
            height = finalHeight,
            colorModel = colorModel,
            defaultColor = config.colors.background
        )

        imageRows.forEachIndexed { rowCount, pixels ->
            result.insertPixels(
                newPixels = pixels,
                startX = 0,
                startY = rowCount * (config.letterHeight + config.lineSpacing)
            )
        }

        return result.asDomainImmutable()
    }
}