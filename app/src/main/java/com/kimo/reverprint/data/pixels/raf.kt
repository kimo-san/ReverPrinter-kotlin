package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.CloseablePixels
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.Pixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class RafBitmapCreator(
    val createFile: () -> File
): MonomodeFabric {

    suspend fun create(
        width: Int,
        height: Int,
        colorModel: ColorModel
    ): Pixels = withContext(Dispatchers.IO) {
        val file = createFile()
        file.createNewFile()
        RafBitmap.writePixelsIntoStorage(
            file, ReadOnlyPixels(
                width = width,
                height = height,
                colorModel = colorModel,
                defaultValue = colorModel.fromModel(Color(0x0), Monochrome)
            )
        )
        RafBitmap(file, createFile)
    }

    private class ReadOnlyPixels(
        override val width: Int,
        override val height: Int,
        colorModel: ColorModel,
        private val defaultValue: Color
    ): AbstractPixels() {

        override var colorModel: ColorModel = colorModel
            set(_) { error("Should not be used") }

        override fun getIntColorForPixel(index: Int): Int {
            return defaultValue.int
        }

        override fun setIntColorForPixel(index: Int, value: Int) {
            error("Should not be used")
        }

        override fun getCopy(): Pixels {
            error("Should not be used")
        }
    }
}

private class RafBitmap(
    private val file: File,
    private val createFile: () -> File
): AbstractPixels(), CloseablePixels {

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Implementation

    override val width: Int get() = header.width
    override val height: Int
        get() = getBitmapHeight().toInt()

    override var colorModel: ColorModel
        get() = header.model.implementedEquivalent
        set(value) {
            val newSavableModel = SavableColorModel.from(value)
            if (newSavableModel == header.model) return
            writeHeader(
                header.copy(KeyInFile.COLOR_MODEL, newSavableModel.code)
            )
        }

    override fun getCopy(): Pixels {
        val copyFile = createFile()
        writePixelsIntoStorage(copyFile, this)
        return RafBitmap(copyFile, createFile)
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // IO

    private val raf = RandomAccessFile(file, "rw")
    override fun close() {
        raf.close()
        file.deleteRecursively()
    }

    override fun setIntColorForPixel(index: Int, value: Int) {
        raf.seek(header.pixelOffset + index.toLong() * Int.SIZE_BYTES)
        raf.writeInt(value)
    }

    override fun getIntColorForPixel(index: Int): Int {
        raf.seek(header.pixelOffset + index.toLong() * Int.SIZE_BYTES)
        return raf.readInt()
    }

    // bitmap is easily extendable by horizontal lines, so
    // the bitmaps of this kind are practically unlimited by height, but by width
    private fun getBitmapHeight(): Long {
        val rowSize = raf.length() - header.pixelOffset
        val bytesPerPixel = Int.SIZE_BYTES
        return rowSize / bytesPerPixel / width
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Metadata

    private val header: FileHeader get() = readHeader()

    private fun readHeader(): FileHeader {
        raf.seek(0)
        val headerLine = raf.readLine() ?: error("File is empty: ${file.absolutePath}")
        val pixelsOffset = raf.filePointer
        return FileHeader.parse(headerLine, pixelsOffset)
    }

    private fun writeHeader(newHeader: FileHeader) {
        raf.seek(0)
        raf.writeBytes(newHeader.pack())
        raf.writeBytes("\n")
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Creation of such bitmap

    companion object {
        fun writePixelsIntoStorage(
            file: File,
            pixels: Pixels
        ) {
            file.deleteRecursively()
            file.createNewFile()
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(0L)
                raf.seek(0L)

                val header = FileHeader.from(
                    pixels.width,
                    SavableColorModel.from(pixels.colorModel)
                )

                raf.writeBytes(header)
                raf.writeBytes("\n")

                for (pixel in pixels.pixelList) {
                    raf.writeInt(pixel)
                }
            }
        }
    }
}

@ConsistentCopyVisibility
private data class FileHeader private constructor(
    val width: Int,
    val model: SavableColorModel,
    val pixelOffset: Long
) {

    fun pack() = from(width, model)
    fun copy(key: KeyInFile, newValue: Int): FileHeader {
        return when (key) {
            KeyInFile.WIDTH ->
                copy(width = newValue)
            KeyInFile.COLOR_MODEL ->
                copy(model = SavableColorModel.Companion.from(newValue))
        }
    }

    companion object {

        fun from(
            width: Int,
            model: SavableColorModel
        ): String = listOf(
            KeyInFile.WIDTH to width,
            KeyInFile.COLOR_MODEL to model.code
        ).joinToString { (key, value) ->
            val key = key.stringName
            val packedValue = IntPacker.pack(value)
            "$key=$packedValue"
        }

        fun parse(line: String, pixelOffset: Long): FileHeader {
            val unpacked = unpackKeys(line)
            return FileHeader(
                width = unpacked[KeyInFile.WIDTH]!!,
                model = SavableColorModel.Companion.from(unpacked[KeyInFile.COLOR_MODEL]!!),
                pixelOffset = pixelOffset
            )
        }

        private fun unpackKeys(string: String): Map<KeyInFile, Int> {

            val allKeysAndValues = Regex("(\\w+)=(.{2})").findAll(string)

            val map = mutableMapOf<KeyInFile, Int>()

            allKeysAndValues.forEachIndexed { i, stringValue ->

                map[KeyInFile.from(stringValue.groupValues[1])] =
                    IntPacker.unpack(stringValue.groupValues[2])
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

// Packs and unpacks any int value into 2 chars
private object IntPacker {

    fun unpack(string: String): Int {
        return (string[0].code shl 16) or string[1].code
    }

    fun pack(int: Int): String {
        val high = (int ushr 16).toChar()
        val low = (int and 0xFFFF).toChar()
        return "$high$low"
    }
}