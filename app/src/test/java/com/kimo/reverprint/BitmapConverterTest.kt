package com.kimo.reverprint

import com.kimo.reverprint.ForBitmapTests.present
import com.kimo.reverprint.domain.images.BitmapSettings
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.extensions.bitmaps.BitmapConverter
import com.kimo.reverprint.extensions.bitmaps.BitmapConverterImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.roundToInt

class BitmapConverterTest {

    @ParameterizedTest
    @MethodSource("randomConverterParameters")
    fun converter(given: ImagePixels, settings: BitmapSettings) = runBlocking {

        val converter: BitmapConverter = BitmapConverterImpl(creator)

        var modified: ImagePixels? = null
        var viewable: ImagePixels? = null

        converter.convertViewableImage(given, settings) { m, v ->
            modified = m
            viewable = v
        }

        val errMsg = {
            println("FAILED TO CONVERT THE FOLLOWING ${given.model} BITMAP ${given.width}x${given.height} to $settings:")
            present(modified!!.pixelList.map {
                it.toHexString(HexFormat {
                    upperCase = true
                    number.removeLeadingZeros = true
                })
            }, BITMAP_SIZE)
            present(viewable!!.pixelList.map { it.toHexString(HexFormat.UpperCase) }, BITMAP_SIZE)
        }

        assert(modified?.model == settings.colorModel) {
            "Color model of modified message (${modified?.model}) does not match to expected one (${settings.colorModel})"
        }
        assert(viewable?.model == DomainColorModel.ARGB_8) {
            "Color model of modified message (${viewable?.model}) does not match to expected one (${settings.colorModel})"
        }

        assert(modified?.height == settings.height && modified?.width == settings.width)
        assert(viewable?.height == settings.height && viewable?.width == settings.width)
        assert(viewable?.height == modified?.height && viewable?.width == modified?.width)

        if (modified?.height == 0 || modified?.width == 0 || given.height == 0 || given.width == 0) {
            assert(modified?.pixelList.isNullOrEmpty())
            assert(viewable?.pixelList.isNullOrEmpty())
            return@runBlocking
        }


        if (given.pixelList.isEmpty()) {
            assert(modified!!.pixelList.isEmpty(), errMsg)
            assert(viewable!!.pixelList.isEmpty(), errMsg)
        }
        else if (given.pixelList.toSet().size > 1) {
            assert(modified!!.pixelList.toSet().size > 1, errMsg)
            assert(viewable!!.pixelList.toSet().size > 1, errMsg)
        }
        else {
            assert(modified!!.pixelList.toSet().size == 1, errMsg)
            assert(viewable!!.pixelList.toSet().size == 1, errMsg)
        }
    }


    companion object {

        @JvmStatic
        fun randomConverterParameters(): List<Arguments> =
            mostRandomBitmaps()
                .map {
                    val settings = BitmapSettings(
                        dither = Math.random().roundToInt() == 1,
                        width = (Math.random() * 0xff).toInt(),
                        height = (Math.random() * 0xff).toInt(),
                        colorModel = DomainColorModel.entries.random()
                    )
                    Arguments.of(it, settings)
                }

    }
}
