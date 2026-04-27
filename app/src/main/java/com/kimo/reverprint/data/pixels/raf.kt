package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.data.pixels.file.FileHeader
import com.kimo.reverprint.data.pixels.file.KeyInFile
import com.kimo.reverprint.data.pixels.file.SavableColorModel
import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.CloseablePixels
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.Pixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

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
            file, StubPixels(
                width = width,
                height = height,
                colorModel = colorModel,
                defaultValue = colorModel.fromModel(Color(0x0), Monochrome)
            )
        )
        RafBitmap(file, createFile)
    }

    private class StubPixels(
        override val width: Int,
        override val height: Int,
        colorModel: ColorModel,
        private val defaultValue: Color
    ): AbstractPixels() {

        override fun getIntColorForPixel(x: Int, y: Int): Int =
            defaultValue.int

        override fun setIntColorForPixel(x: Int, y: Int, value: Int) = error("Should not be used")
        override fun getCopy(): Pixels = error("Should not be used")
        override var colorModel: ColorModel = colorModel
            set(_) = error("Should not be used")
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
        val newFile = createFile()
        Files.copy(file.toPath(), newFile.toPath())
        return RafBitmap(file, createFile)
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // IO

    private val raf = RandomAccessFile(file, "rw")
    override fun close() {
        raf.close()
        file.deleteRecursively()
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        raf.seek(header.pixelOffset + indexOf(x, y).toLong() * Int.SIZE_BYTES)
        raf.writeInt(value)
    }

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        require(x in 0 until width) { "The width is $width" }
        require(y in 0 until height) { "The height is $height" }
        raf.seek(header.pixelOffset + indexOf(x, y).toLong() * Int.SIZE_BYTES)
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
            val header = FileHeader.from(
                pixels.width,
                SavableColorModel.from(pixels.colorModel)
            ).pack()
            file.writer().use {
                it.write(header, 0, header.length)
            }
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(0L)
                raf.seek(0L)


                raf.writeBytes(header)
                raf.writeBytes("\n")

                for (pixel in pixels.pixelList) {
                    raf.writeInt(pixel)
                }
            }
        }
    }
}