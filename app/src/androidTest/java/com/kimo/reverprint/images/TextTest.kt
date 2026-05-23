package com.kimo.reverprint.images

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kimo.reverprint.ForBitmapTests
import com.kimo.reverprint.ForFontTests
import com.kimo.reverprint.extensions.bitmaps.text.BitmapTextConfig
import com.kimo.reverprint.extensions.bitmaps.text.TextOnBitmapGeneratorImpl
import com.kimo.reverprint.getContext
import com.kimo.reverprint.tools.fonts.ColorSettings
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Monochrome
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TextTest {

    private val context: Context = getContext()
    private val textSettings = BitmapTextConfig(
        width = 50,
        letterHeight = 10,
        letterSpacing = 0,
        lineSpacing = 0,
        colors = ColorSettings(
            model = Monochrome,
            foreground = Color(0x1),
            background = Color(0x0)
        ),
        font = ForFontTests.TestFont()
    )

    private fun getFileName(testNumber: Int, caseNumber: Int) =
        "tested_bitmap_${testNumber}_$caseNumber.bin"

    @Test
    fun textG(): Unit = runBlocking {

        val f1 = File(context.filesDir, getFileName(1, 1)).also { it.createNewFile() }
        val creator = ForBitmapTests.bitmapCreator { f1 }
        val textGen = TextOnBitmapGeneratorImpl(creator)

        println("Created file: " + f1.length() + " " + f1.exists())

        val px1 = textGen.generatePixels("A", textSettings)
        println("Pixels: ${px1.model}, ${px1.width}, ${px1.height}. Array Size: ${px1.pixelList.size}")
        ForBitmapTests.present(px1.pixelList, px1.width)
    }

//    @Test
//    fun textGen() {
//        val testNumber = 2
//        runBlocking {
//            val px1 = creator.from(px)
//            val f2 = File(context.filesDir, getFileName(testNumber, 2))
//            val creator2 = BitmapFabric { f2 }
//            val px2 = creator2.create(
//                BitmapConfig(
//                    px.width, null,
//                    px.model.implementedEquivalent(),
//                    StorageType.MAPPED_RAF
//                )
//            )
//            px2.insertPixels(px1, 0, 0)
//            println("\n\n\n ##### NEXT ##### \n\n\n")
//            ForBitmapTests.present(px2.pixelList, px2.width)
//            px2.changeColorModel(Argb8)
//            println("\n\n\n ##### NEXT ##### \n\n\n")
//            ForBitmapTests.present(px2.pixelList, px2.width)
//        }
//    }
}