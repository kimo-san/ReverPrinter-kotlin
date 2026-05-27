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
import com.kimo.reverprint.tools.font.Font
import com.kimo.reverprint.tools.font.FontParameters
import com.kimo.reverprint.tools.font.Glyph
import com.kimo.reverprint.extensions.bitmaps.implementedEquivalent
import com.kimo.reverprint.extensions.bitmaps.insertFrom
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.StorageType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap

// todo: remove runBlocking calls
class LoadedFontImpl(
    private val context: Context,
    private val bitmapCreator: BitmapCreator
): Font {

    fun loadTypeface(): Typeface {
        return ResourcesCompat.getFont(context, R.font.archivo_medium)!!
    }

    private val typeface = loadTypeface()
    suspend fun drawGlyph(
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

        val bp = bitmap.toImagePixels()
        return Glyph(
            bitmapCreator.create(
                BitmapConfig(
                    bp.width,
                    bp.height,
                    bp.model.implementedEquivalent(),
                    StorageType.NATIVE
                )
            ).insertFrom(bp)
        )
    }

    override fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph = runBlocking { buffer.getGlyph(char, parameters) }

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
    private val getNewItem: suspend (Char, FontParameters) -> Glyph
) {

    private var fontParams: FontParameters? = null
    private var buffer = ConcurrentHashMap<Char, Glyph>()

    suspend fun getGlyph(
        char: Char,
        parameters: FontParameters
    ): Glyph {
        yield()
        if (fontParams != parameters) {
            buffer.clear()
        }
        return buffer.computeIfAbsent(char) {
            runBlocking {
                getNewItem(char, parameters)
            }
        }
    }
}