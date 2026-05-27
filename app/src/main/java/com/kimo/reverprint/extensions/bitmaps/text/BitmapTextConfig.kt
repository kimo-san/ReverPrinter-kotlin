package com.kimo.reverprint.extensions.bitmaps.text

import com.kimo.reverprint.tools.font.ColorSettings
import com.kimo.reverprint.tools.font.Font

data class BitmapTextConfig (
    val width: Int,
    val letterHeight: Int,
    val letterSpacing: Int,
    val lineSpacing: Int,
    val colors: ColorSettings,
    val font: Font
)