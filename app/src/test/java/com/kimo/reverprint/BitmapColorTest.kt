package com.kimo.reverprint

import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import kotlin.math.roundToInt

class BitmapColorTest {

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
        TODO()
    }

    @ParameterizedTest
    @CsvSource(
        "15, 0, 255, 170, 0.7633333",
        "10, 0, 255, 170, 0.7633333",
        "0, 0, 255, 170, 0.7633333",
        "15, 0, 0, 0, 0",
        "10, 0, 0, 0, 0",
        "0, 0, 0, 0, 0",
        "15, 255, 255, 255, 1",
        "10, 255, 255, 255, 1",
        "0, 255, 255, 255, 1",
    )
    fun luminanceArgb8(a: Int, r: Int, g: Int, b: Int, expect: Float) {
        val model = Argb8 as ColorModel
        val color = Argb8.colorOf(a, r, g, b)
        val lum = model.lumOf(color)
        assert(lum == expect) {
            println("Calculated lum of ARGB($a|$r|$g|$b) is $lum. Expected $expect.")
        }
    }

    @ParameterizedTest
    @MethodSource("randomColors")
    fun convertToGrey8(srcModel: ColorModel, srcColor: Color) {

        val srcLum = srcModel.lumOf(srcColor)

        val trgModel = Grey8 as ColorModel
        val trgColor = trgModel.colorOf((srcLum * (trgModel.channelDepth - 1)).roundToInt())

        val actualColor = trgModel.fromModel(srcColor, srcModel)

        assert(actualColor.int == trgColor.int) {
            println("Calculated Grey8 color from ARGB(${srcColor.hex}) is Grey8(${actualColor.int}). Expecting Grey8(${trgColor.int}), luminance is $srcLum.")
        }
    }

    @ParameterizedTest
    @MethodSource("randomColors")
    fun convertToGrey4(srcModel: ColorModel, srcColor: Color) {

        val srcLum = srcModel.lumOf(srcColor)

        val trgModel = Grey4 as ColorModel
        val trgColor = trgModel.colorOf((srcLum * (trgModel.channelDepth - 1)).roundToInt())

        val actualColor = trgModel.fromModel(srcColor, srcModel)

        assert(actualColor.int == trgColor.int) {
            println("Calculated Grey8 color from ARGB(${srcColor.hex}) is Grey4(${actualColor.int}). Expecting Grey4(${trgColor.int}).")
        }
    }

    @ParameterizedTest
    @CsvSource(
        "182, 11",
        "0, 0",
        "255, 15",
    )
    fun convertGrey8ToGrey4(grey8value: Int, expectGrey4value: Int) {

        val srcModel = Grey8 as ColorModel
        val srcColor = Grey8.colorOf(grey8value)

        val trgModel = Grey4 as ColorModel
        val trgColor = Grey4.colorOf(expectGrey4value)

        val actualColor = trgModel.fromModel(srcColor, srcModel)

        assert(actualColor.int == trgColor.int) {
            println("Calculated Grey4 color from Grey8(${srcColor.int}) is Grey4(${actualColor.int}). Expecting Grey4(${trgColor.int}).")
        }
    }

    companion object {

        @JvmStatic
        fun argbColors(): List<Arguments> = mutableListOf(
            Argb8.colorOf(15, 0, 255, 170),
            Argb8.colorOf(0, 0, 255, 170),
            Argb8.colorOf(15, 0, 0, 0),
            Argb8.colorOf(0, 0, 0, 0),
            Argb8.colorOf(15, 255, 255, 255),
            Argb8.colorOf(0, 255, 255, 255),
        ).apply {
            repeat(1000) { add(generateRandomColor(Argb8)) }
        }.map { Arguments.of(Argb8, it.int) }


        @JvmStatic
        fun randomColors(): List<Arguments> = mostRandomColors()
            .map { Arguments.of(it.first, it.second.int) }

    }
}