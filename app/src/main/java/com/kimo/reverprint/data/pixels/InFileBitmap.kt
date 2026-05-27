package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.file.IFile
import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType

private const val BYTES_PER_PIXEL = Int.SIZE_BYTES
class InFileBitmap(
    private val mappedFile: IFile
) : AbstractPixels() {

    override val storageType: StorageType get() = StorageType.FILE

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Stored data

    private var header: FileHeader = readHeader()
    private val usedPixelsCount get() = mappedFile.size - header.pixelOffset

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Handle header

    private fun readHeader(): FileHeader {
        val headerLine = mappedFile.readFirstLine() ?: error("File is empty: $mappedFile")
        val pixelsOffset = mappedFile.readFirstLineByteSize()
        return FileHeader.Companion.parse(headerLine, pixelsOffset)
    }

    private fun writeHeader(newHeader: FileHeader) {
        mappedFile.writeBytes(0, newHeader.pack())
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Getters

    override val width: Int
        get() = header.width

    override val height: Int
        get() = (usedPixelsCount / header.width).toInt().coerceAtLeast(1)

    override var colorModel: ColorModel
        get() = header.colorModel
        set(value) {
            writeHeader(header.withRewrittenColorModel(value))
        }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Input / output

    private fun indexInFile(x: Int, y: Int) =
        indexOf(x, y) * BYTES_PER_PIXEL + header.pixelOffset

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y in 0 ..< height) { "y out of bounds: $y, height=${height - 1}" }
        return mappedFile.readInt(indexInFile(x, y))
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y >= 0) { "y < 0: $y" }
        mappedFile.writeInt(indexInFile(x, y), value)
    }

    override fun getCopy(): Pixels {
        return InFileBitmap(mappedFile.copy())
    }


    // - - - - - - - - - - - - - - - - - - - - - - -
    // Init

    companion object {
        fun writePixelsIntoStorage(
            file: IFile,
            header: FileHeader
        ) {
            file.clean()
            val packed = header.pack()
            file.writeBytes(0, packed)
        }
    }
}