package com.kimo.reverprint

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kimo.reverprint.data.pixels.BitmapFabric
import com.kimo.reverprint.extensions.bitmaps.text.BitmapTextConfig
import com.kimo.reverprint.extensions.bitmaps.text.TextOnBitmapGeneratorImpl
import com.kimo.reverprint.extensions.bitmaps.from
import com.kimo.reverprint.extensions.bitmaps.implementedEquivalent
import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.fonts.Font
import com.kimo.reverprint.tools.fonts.FontParameters
import com.kimo.reverprint.tools.fonts.Glyph
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.StorageType
import com.kimo.reverprint.tools.graphics.insertPixels
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RafImageTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testInput() = runBlocking {

        val file = File(context.filesDir, "saved_bitmap_1.bin")
        val crFile = { file }

        println("Created file: " + file.createNewFile())

        val creator = BitmapFabric(crFile)
        val sourcePixels = creator.from(createChessBitmap(100, 100, 2))
        println(file.readText())

        println("Attempting to write into file...")
        val loadable = creator.create(
            BitmapConfig(
                sourcePixels.width,
                null,
                sourcePixels.colorModel,
                StorageType.MAPPED_RAF
            )
        )
        loadable.insertPixels(sourcePixels, 0, 0)

        println("Attempting to read the file again... " + file.length())
        println(file.readText())
        println("Attempting to read metadata...")
        println("SOOO: width: " + loadable.width + ", height: " + loadable.height)

        present(loadable.pixelList, loadable.width)
    }

    @Test
    fun textGen() {
        runBlocking {

            val f1 = File(context.filesDir, "saved_bitmap_2_1.bin")
            val creator = BitmapFabric { f1 }
            val textGen = TextOnBitmapGeneratorImpl(creator)

            println("Created file: " + f1.length() + " " + f1.exists())

            val px = textGen.generatePixels(
                "A", BitmapTextConfig(
                    width = 100,
                    letterHeight = 50,
                    letterSpacing = 0,
                    lineSpacing = 0,
                    colors = ColorSettings(
                        model = Monochrome,
                        foreground = Color(0x1),
                        background = Color(0x0)
                    ),
                    font = BaseFont()
                )
            )
            val px1 = creator.from(px)
            present(px1.pixelList, px1.width)

            val f2 = File(context.filesDir, "saved_bitmap_2_2.bin")
            val creator2 = BitmapFabric { f2 }
            val px2 = creator2.create(
                BitmapConfig(
                    px.width, null,
                    px.model.implementedEquivalent(),
                    StorageType.MAPPED_RAF
                )
            )
            px2.insertPixels(px1, 0, 0)
            println("\n\n\n ##### NEXT ##### \n\n\n")
            present(px2.pixelList, px2.width)
            px2.changeColorModel(Argb8)
            println("\n\n\n ##### NEXT ##### \n\n\n")
            present(px2.pixelList, px2.width)
        }
    }

}

class BaseFont: Font {
    override fun getBitmapOfChar(
        char: Char,
        parameters: FontParameters
    ): Glyph {
        return gph
    }

    val gph = runBlocking {
        Glyph(
            BitmapFabric { error("slop") }.from(createChessBitmap(4, 4, 2))
        )
    }
}