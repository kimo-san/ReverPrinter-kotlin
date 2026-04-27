package com.kimo.reverprint.extensions.bitmaps.text

import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.extensions.bitmaps.asDomainImmutable
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.fullFill
import com.kimo.reverprint.tools.graphics.insertPixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class TextOnBitmapGeneratorImpl(
    private val creator: BitmapCreator
): TextOnBitmapGenerator {

    override suspend fun generatePixels(
        text: String,
        config: BitmapTextConfig
    ): ImagePixels = withContext(Dispatchers.Default) {

        val imageRows = mutableListOf<Pixels>()

        var xPointer = 0
        val yPointer = 0
        var currentRow = 0

        fun nextRow() {
            currentRow++
            xPointer = 0
        }
        suspend fun presentRow() =
            imageRows.getOrElse(currentRow) {
                do {
                    imageRows += creator.create(
                        width = config.width,
                        height = config.letterHeight,
                        colorModel = config.colors.model,
                    ).apply {
                        fullFill(config.colors.background)
                    }
                } while (imageRows.lastIndex < currentRow)
                imageRows[currentRow]
            }

        text.forEach { char ->
            yield()
            when (char) {
                '\n' -> { nextRow() }
                else -> {

                    val toInsert = config.font.getBitmapOfChar(
                        char, FontParameters(
                            config.letterHeight,
                            config.colors))

                    if (xPointer + toInsert.bitmap.width > presentRow().width) {
                        nextRow()
                    }

                    presentRow().insertPixels(
                        newPixels = toInsert.bitmap,
                        startX = xPointer,
                        startY = yPointer
                    )

                    xPointer += toInsert.bitmap.width + config.letterSpacing
                }
            }
        }

        val result = creator.createExtendable(
            width = config.width,
            colorModel = config.colors.model
        )

        val spacing = creator.create(
            config.width, config.lineSpacing, config.colors.model
        ).apply { fullFill(config.colors.background) }
        repeat(imageRows.size) { rowCount ->
            result.insertPixels(
                newPixels = imageRows[rowCount],
                startX = 0,
                startY = rowCount * (config.letterHeight + config.lineSpacing)
            )
            result.insertPixels(
                spacing,
                0,
                rowCount * (config.letterHeight + config.lineSpacing) + config.letterHeight
            )
        }

        return@withContext result.asDomainImmutable()
    }
}