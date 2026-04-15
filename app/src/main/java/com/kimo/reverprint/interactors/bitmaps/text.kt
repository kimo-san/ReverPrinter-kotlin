package com.kimo.reverprint.interactors.bitmaps

import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.graphics.Pixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val colors: ColorSettings,
    val font: Font
)

private class TextOnBitmapGeneratorImpl : TextOnBitmapGenerator {
    override suspend fun placeTextOnBitmap(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels = withContext(Dispatchers.Default) {

        val imageRows = mutableListOf<Pixels>()

        var currentRowStart = 0
        var currentRow = 0

        fun presentRow() =
            imageRows.getOrElse(currentRow) {
                do {
                    imageRows += Pixels(
                        width = config.width,
                        height = config.letterHeight,
                        colorModel = config.colors.model,
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
                                colors = config.colors
                            )
                        )

                    if (currentRowStart + toInsert.bitmap.width > presentRow().width + config.letterSpacing) {
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
            colorModel = config.colors.model,
            defaultColor = config.colors.background
        )

        imageRows.forEachIndexed { rowCount, pixels ->
            result.insertPixels(
                newPixels = pixels,
                startX = 0,
                startY = rowCount * (config.letterHeight + config.lineSpacing)
            )
        }

        return@withContext result.asDomainImmutable()
    }
}