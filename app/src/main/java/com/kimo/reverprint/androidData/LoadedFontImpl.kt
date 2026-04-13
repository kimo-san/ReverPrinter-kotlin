package com.kimo.reverprint.androidData

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.kimo.reverprint.R
import com.kimo.reverprint.interactors.bitmapPlayground.Argb8
import com.kimo.reverprint.interactors.bitmapPlayground.Font
import com.kimo.reverprint.interactors.bitmapPlayground.FontParameters
import com.kimo.reverprint.interactors.bitmapPlayground.Glyph
import com.kimo.reverprint.interactors.bitmapPlayground.Monochrome
import com.kimo.reverprint.interactors.bitmapPlayground.Pixels
import com.kimo.reverprint.tools.graphics.Color

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
        backgroundIntColor: Int,
        foregroundIntColor: Int
    ): Glyph {

        val paint = Paint().apply {
            typeface = this@LoadedFontImpl.typeface
            textSize = height.toFloat()
            isAntiAlias = true
            color = foregroundIntColor
        }
        val metrics = paint.fontMetrics

        val width = paint.measureText(char.toString()).toInt()
        val height = height

        val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundIntColor)

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
//            Color.BLACK, Color.WHITE
            Argb8.fromModel(Color(0x1), Monochrome).int,
            Argb8.fromModel(Color(0x0), Monochrome).int
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