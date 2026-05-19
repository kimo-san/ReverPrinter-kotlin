package com.kimo.reverprint.extensions.bitmaps.text

import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.extensions.bitmaps.asDomainImmutable
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.StorageType
import com.kimo.reverprint.tools.graphics.fillRectangle
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

        val result = creator.create(
            BitmapConfig(
                width = config.width,
                colorModel = config.colors.model,
                storage = StorageType.MAPPED_RAF
            )
        )

        var xPointer = 0
        var yPointer = 0

        suspend fun nextRow() {
            // fill rest of the current row
            result.fillRectangle(
                config.colors.background,
                config.colors.model,
                startX = xPointer,
                endX = result.width - 1,
                startY = yPointer,
                endY = result.height
            )
            // move pointers
            xPointer = 0
            yPointer += config.letterHeight + config.lineSpacing
        }

        text.forEach { char ->
            yield()
            when (char) {
                '\n' -> { nextRow() }
                else -> {

                    val toInsert = config.font.getBitmapOfChar(
                        char, FontParameters(
                            config.letterHeight,
                            config.colors)
                    )

                    val spaceWithChar = xPointer + toInsert.bitmap.width
                    if (spaceWithChar > config.width) {
                        nextRow()
                    }

                    result.insertPixels(
                        newPixels = toInsert.bitmap,
                        startX = xPointer,
                        startY = yPointer
                    )

                    xPointer += toInsert.bitmap.width
                }
            }
        }

        nextRow()

        return@withContext result.asDomainImmutable()
    }
}