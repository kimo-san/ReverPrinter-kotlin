package com.kimo.reverprint

import com.kimo.reverprint.ForBitmapTests.createChessBitmap
import com.kimo.reverprint.data.pixels.BitmapFabric
import com.kimo.reverprint.extensions.bitmaps.from
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.fonts.Glyph
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.StorageType
import kotlinx.coroutines.runBlocking

class TestFont: Font {

    override fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph {
        if (char == ' ') return spaceGlyph
        return defaultGlyph
    }


    private val fabric = BitmapFabric { error("slop") }

    private val defaultGlyph = runBlocking {
        createChessBitmap(4, 4, 2)
            .let { fabric.from(it) }
            .let(::Glyph)
    }

    private val spaceGlyph = runBlocking {
        BitmapConfig(4, 4, Monochrome, StorageType.RAM)
            .let { fabric.create(it) }
            .let(::Glyph)
    }
}