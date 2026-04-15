package com.kimo.reverprint.tools.fonts

interface Font {
    fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph
}