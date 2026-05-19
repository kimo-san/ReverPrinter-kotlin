package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome

private const val ASCII_CHAR_LENGTH = 1
@ConsistentCopyVisibility
data class FileHeader private constructor(
    val width: Int,
    private val model: SavableColorModel,
    val pixelOffset: Long,
    private val packed: String
) {
    val colorModel get() = model.implementedEquivalent

    fun pack() = packed.toByteArray(Charsets.US_ASCII)

    fun withRewrittenColorModel(newModel: ColorModel) =
        copy(model = SavableColorModel.from(newModel))

    companion object {

        fun from(
            width: Int,
            model: ColorModel
        ): FileHeader {

            val savableModel = SavableColorModel.from(model)
            val packed = listOf(
                KeyInFile.WIDTH to width,
                KeyInFile.COLOR_MODEL to savableModel.code
            ).joinToString(
                separator = ",",
                postfix = "\n"
            ) { (key, value) ->
                "${key.stringName}=$value"
            }

            return FileHeader(
                width = width,
                model = savableModel,
                packed = packed,
                pixelOffset = packed.length.toLong() * ASCII_CHAR_LENGTH
            )
        }

        fun parse(line: String, pixelOffset: Long): FileHeader {
            val unpacked = unpackKeys(line)
            return FileHeader(
                width = unpacked[KeyInFile.WIDTH]!!,
                model = SavableColorModel.from(unpacked[KeyInFile.COLOR_MODEL]!!),
                pixelOffset = pixelOffset,
                packed = line,
            )
        }

        private fun unpackKeys(string: String): Map<KeyInFile, Int> {

            val allKeysAndValues = Regex("(\\w+)=(\\d+)").findAll(string)
            val map = mutableMapOf<KeyInFile, Int>()

            allKeysAndValues.forEach { encodedValues ->
                val key = encodedValues.groupValues[1]
                val encodedValue = encodedValues.groupValues[2]
                map[KeyInFile.from(key)] = encodedValue.toInt()
            }

            return map
        }
    }
}

private enum class KeyInFile(val stringName: String) {

    WIDTH("w"),
    COLOR_MODEL("c");

    companion object {
        private val byString: Map<String, KeyInFile> =
            entries.associateBy { it.stringName }
        fun from(string: String): KeyInFile =
            byString[string] ?: error("Unsupported code of KeyInFile: $string")
    }
}

private enum class SavableColorModel(
    val implementedEquivalent: ColorModel,
    val code: Int
) {
    ARGB8(Argb8, 1),
    GREY8(Grey8, 2),
    GREY4(Grey4, 3),
    MONO(Monochrome, 4);

    companion object {
        private val byModel: Map<ColorModel, SavableColorModel> =
            entries.associateBy { it.implementedEquivalent }
        private val byString: Map<Int, SavableColorModel> =
            entries.associateBy { it.code }

        fun from(model: ColorModel): SavableColorModel =
            byModel[model] ?: error("Unsupported ColorModel: $model")
        fun from(code: Int): SavableColorModel =
            byString[code] ?: error("Unsupported code of color model: $code")
    }
}