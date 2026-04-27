package com.kimo.reverprint.data.pixels.file

import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome

@ConsistentCopyVisibility
data class FileHeader private constructor(
    val width: Int,
    val model: SavableColorModel,
    val pixelOffset: Long,
    private val packed: String
) {

    fun pack() = packed

    fun copy(key: KeyInFile, newValue: Int): FileHeader {
        return when (key) {
            KeyInFile.WIDTH ->
                copy(width = newValue)
            KeyInFile.COLOR_MODEL ->
                copy(model = SavableColorModel.from(newValue))
        }
    }

    companion object {

        fun from(
            width: Int,
            model: SavableColorModel
        ): FileHeader {
            val packed = listOf(
                KeyInFile.WIDTH to width,
                KeyInFile.COLOR_MODEL to model.code
            ).joinToString(
                separator = ",",
                postfix = "\n"
            ) { (key, value) ->
                val key = key.stringName
                val packedValue = IntEncoder.encode(value)
                "$key=$packedValue"
            }
            return FileHeader(
                width = width,
                model = model,
                pixelOffset = packed.length.toLong() * Char.SIZE_BYTES,
                packed
            )
        }

        fun parse(line: String, pixelOffset: Long): FileHeader {
            val unpacked = unpackKeys(line)
            return FileHeader(
                width = unpacked[KeyInFile.WIDTH]!!,
                model = SavableColorModel.from(unpacked[KeyInFile.COLOR_MODEL]!!),
                pixelOffset = pixelOffset,
                line
            )
        }

        private fun unpackKeys(string: String): Map<KeyInFile, Int> {

            val allKeysAndValues = Regex("(\\w+)=(.{2})").findAll(string)
            val map = mutableMapOf<KeyInFile, Int>()

            allKeysAndValues.forEach { encodedInt ->
                val key = encodedInt.groupValues[1]
                val encodedValue = encodedInt.groupValues[2]
                map[KeyInFile.from(key)] = IntEncoder.decode(encodedValue)
            }

            return map
        }
    }
}

enum class KeyInFile(val stringName: String) {

    WIDTH("w"),
    COLOR_MODEL("c");

    companion object {
        private val byString: Map<String, KeyInFile> =
            entries.associateBy { it.stringName }
        fun from(string: String): KeyInFile =
            byString[string] ?: error("Unsupported code of KeyInFile: $string")
    }
}

enum class SavableColorModel(
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

// Packs and unpacks any int value into 2 characters
private object IntEncoder {

    fun decode(string: String): Int {
        return (string[0].code shl 16) or string[1].code
    }

    fun encode(int: Int): String {
        val high = (int ushr 16).toChar()
        val low = (int and 0xFFFF).toChar()
        return "$high$low"
    }
}