package com.kimo.reverprint.tools.font

import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels

interface Font {
    fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph
}

@JvmInline
value class Glyph (val bitmap: Pixels)

class ColorSettings (
    val model: ColorModel,
    val foreground: Color,
    val background: Color,
)

data class FontParameters(
    val size: Int,
    val colors: ColorSettings
)