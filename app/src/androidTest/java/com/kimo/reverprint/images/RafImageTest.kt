package com.kimo.reverprint.images

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kimo.reverprint.ForBitmapTests
import com.kimo.reverprint.ForBitmapTests.bitmapCreator
import com.kimo.reverprint.extensions.bitmaps.from
import com.kimo.reverprint.tools.graphics.BitmapConfig
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

        val creator = bitmapCreator(crFile)
        val sourcePixels = creator.from(ForBitmapTests.createChessBitmap(100, 100, 2))
        println(file.readText())

        println("Attempting to write into file...")
        val loadable = creator.create(
            BitmapConfig(
                sourcePixels.width,
                null,
                sourcePixels.colorModel,
                StorageType.MAPPED_FILE
            )
        )
        loadable.insertPixels(sourcePixels, 0, 0)

        println("Attempting to read the file again... " + file.length())
        println(file.readText())
        println("Attempting to read metadata...")
        println("SOOO: width: " + loadable.width + ", height: " + loadable.height)

        ForBitmapTests.present(loadable.pixelList, loadable.width)
    }
}