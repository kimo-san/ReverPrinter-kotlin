package com.kimo.reverprint.domain.images

import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.fonts.Font

data class BitmapTextConfig (
    val width: Int,
    val letterHeight: Int,
    val letterSpacing: Int,
    val lineSpacing: Int,
    val colors: ColorSettings,
    val font: Font
)