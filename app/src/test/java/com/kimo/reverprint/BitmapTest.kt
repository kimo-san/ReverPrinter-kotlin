package com.kimo.reverprint

import com.kimo.reverprint.ForBitmapTests.createGradientBitmap
import com.kimo.reverprint.ForBitmapTests.present
import com.kimo.reverprint.domain.images.BitmapSettings
import com.kimo.reverprint.domain.images.ColorModel
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.extensions.bitmaps.BitmapConverter
import com.kimo.reverprint.extensions.bitmaps.BitmapConverterImpl
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.extensions.bitmaps.asDomainImmutable
import com.kimo.reverprint.extensions.bitmaps.from
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType
import com.kimo.reverprint.tools.graphics.fullFill
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class BitmapTest {

    companion object {

        val size = 10
        val creator = ForBitmapTests.ramOnlyCreator()
        val converter: BitmapConverter = BitmapConverterImpl(creator)
        lateinit var filled: Pixels
        lateinit var gradient: Pixels

        @JvmStatic
        @BeforeClass
        fun prepare(): Unit = runBlocking {
            launch {
                filled = creator.create(
                    BitmapConfig(
                        size, size, Argb8,
                        StorageType.RAM
                    )
                ).apply {
                    fullFill(Color(0xFF00FFAAL.toInt()))
                }
            }
            launch {
                gradient = creator.from(createGradientBitmap(size, size))
            }
        }
    }

    @Before
    fun note() = println("\n=== NEXT TEST ===")

    @Test
    fun luminance() {
        val lums = filled.pixelList.map { filled.colorModel.lumOf(Color(it)) }
        present(lums, filled.width)
        assert(lums.all { it == 0.7633333f })
    }

    @Test
    fun channels() {

        val channelValues = filled.pixelList.map { col ->
            val values = mutableListOf<Int>()
            repeat(filled.colorModel.channelCount) { ch ->
                values += filled.colorModel.getChannelValue(Color(col), ch)
            }
            values.joinToString(
                postfix = ")",
                prefix = "(",
                separator = "|"
            ) { "$it" }
        }
        assert(channelValues.all { it == "(255|0|255|170)" })
        present(channelValues, filled.width)
    }

    @Test
    fun toGrey() = runBlocking {
        val given = gradient
        given.changeColorModel(Grey4)
        present(given.pixelList.toIntArray(), given.width)
        assert(given.pixelList.toSet().size != 2)
    }

    @Test
    fun converter() = runBlocking {
        val given = gradient.asDomainImmutable()
        var mod: ImagePixels? = null
        var vie: ImagePixels? = null
        converter.convertViewableImage(
            given,
            BitmapSettings(
                colorModel = ColorModel.GREY_4BPP,
                dither = true
            )
        ) { m, v -> mod = m; vie = v }
        present(mod!!.pixelList, size)
        present(vie!!.pixelList, size)
    }
}
