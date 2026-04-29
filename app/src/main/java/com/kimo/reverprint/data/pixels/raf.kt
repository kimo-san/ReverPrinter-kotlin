package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.CloseablePixels
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType
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
        colorModel: ColorModel
    ): Pixels = withContext(Dispatchers.IO) {
        val file = createFile()
        RafBitmap.writePixelsIntoStorage(
            file,
            FileHeader.from(width, colorModel)
        )
        RafBitmap(file, createFile)
    }
}

private class RafBitmap(
    private val file: File,
    private val createFile: () -> File
): AbstractPixels(), CloseablePixels {

    override val storageType: StorageType = StorageType.RAF

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Implementation

    override val width: Int get() = header.width
    override val height: Int
        get() = getBitmapHeight().toInt()

    override var colorModel: ColorModel
        get() = header.colorModel
        set(value) {
            writeHeader(header.withRewrittenColorModel(value))
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
        raf.seek(header.pixelOffset + indexOf(x, y) * Int.SIZE_BYTES)
        raf.writeInt(value)
    }

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        require(x in 0 until width) { "The width is $width" }
        require(y in 0 until height) { "The height is $height" }
        raf.seek(header.pixelOffset + indexOf(x, y) * Int.SIZE_BYTES)
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
        raf.readLine()
        val headerLine = raf.readLine() ?: error("File is empty: ${file.absolutePath}")
        val pixelsOffset = raf.filePointer
        return FileHeader.parse(headerLine, pixelsOffset)
    }

    private fun writeHeader(newHeader: FileHeader) {
        raf.seek(0)
        raf.write(newHeader.pack())
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Creation of such bitmap

    companion object {
        fun writePixelsIntoStorage(
            file: File,
            header: FileHeader
        ) {
            file.delete()
            file.createNewFile()
            val packed = header.pack()
            file.writeBytes(packed)
        }
    }
}