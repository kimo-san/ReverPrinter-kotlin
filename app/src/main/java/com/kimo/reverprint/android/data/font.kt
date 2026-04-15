package com.kimo.reverprint.android.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.kimo.reverprint.R
import com.kimo.reverprint.android.toImagePixels
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.fonts.Glyph
import com.kimo.reverprint.interactors.bitmaps.Pixels

class LoadedFontImpl(
    private val context: Context
): Font {

    fun loadTypeface(): Typeface {
        return ResourcesCompat.getFont(context, R.font.archivo_medium)!!
    }

    private val typeface = loadTypeface()
    fun drawGlyph(
        char: Char,
        height: Int,
        backgroundArgbColor: Int,
        foregroundArgbColor: Int
    ): Glyph {

        val paint = Paint().apply {
            typeface = this@LoadedFontImpl.typeface
            textSize = height.toFloat()
            isAntiAlias = true
            color = foregroundArgbColor
        }
        val metrics = paint.fontMetrics

        val width = paint.measureText(char.toString()).toInt()
        val height = height

        val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundArgbColor)

        canvas.drawText(
            char.toString(),
            0f,
            -metrics.ascent,
            paint
        )

        return Glyph(Pixels(bitmap.toImagePixels()))
    }

    override fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph = buffer.getGlyph(char, parameters)

    private val buffer = ParametrizedGlyphBuffer { char, parms ->
        drawGlyph(
            char,
            parms.size,
            Argb8.fromModel(parms.colors.background, parms.colors.model).int,
            Argb8.fromModel(parms.colors.foreground, parms.colors.model).int
        )
    }

}

private class ParametrizedGlyphBuffer (
    private val getNewItem: (Char, FontParameters) -> Glyph
) {

    private var currentParameters: FontParameters? = null
    private var buffer = mutableMapOf<Char, Glyph>()

    fun getGlyph(
        char: Char,
        parameters: FontParameters
    ): Glyph {

        if (parameters != currentParameters) {
            buffer = mutableMapOf()
            currentParameters = parameters
        }

        if (buffer[char] == null) {
            buffer[char] = getNewItem(char, parameters)
        }

        return buffer[char]!!
    }
}