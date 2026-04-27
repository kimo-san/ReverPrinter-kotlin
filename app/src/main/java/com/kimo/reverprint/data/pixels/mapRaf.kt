package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.data.pixels.file.FileHeader
import com.kimo.reverprint.data.pixels.file.KeyInFile
import com.kimo.reverprint.data.pixels.file.SavableColorModel
import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.CloseablePixels
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.math.max

private const val GROWTH_BLOCK_BYTES = 1024 * 1024
private const val BYTES_PER_PIXEL = Int.SIZE_BYTES
class MappedRafBitmapCreator(
    val createFile: () -> File
): MonomodeFabric {

    suspend fun create(
        width: Int,
        colorModel: ColorModel
    ): Pixels = withContext(Dispatchers.IO) {
        val file = createFile()
        MappedRafBitmap.writePixelsIntoStorage(
            file,
            FileHeader.from(
                width,
                SavableColorModel.from(colorModel)
            )
        )
        MappedRafBitmap(file, createFile)
    }
}


private class MappedRafBitmap(
    private val file: File,
    private val createFile: () -> File
) : AbstractPixels(), CloseablePixels {

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Raf + Channel logic

    private val raf = RandomAccessFile(file, "rw")
    private val channel: FileChannel = raf.channel
    private var header: FileHeader = readHeader()
    private var mapped: MappedByteBuffer = mapData(header.pixelOffset)
    private var dataCapacityBytes: Long =
        (file.length() - header.pixelOffset).coerceAtLeast(0L)

    private fun mapData(dataCapacityBytes: Long): MappedByteBuffer {
        val safeCapacity = dataCapacityBytes.coerceAtLeast(GROWTH_BLOCK_BYTES.toLong())
        val requiredFileSize = header.pixelOffset + safeCapacity

        if (file.length() < requiredFileSize) {
            raf.setLength(requiredFileSize)
        }

        return channel.map(
            FileChannel.MapMode.READ_WRITE,
            header.pixelOffset,
            safeCapacity
        )
    }

    // capacity logic

    private fun ensureCapacityFor(requiredPixelCount: Long) {
        val requiredBytes = requiredPixelCount * BYTES_PER_PIXEL
        if (requiredBytes <= dataCapacityBytes) return

        val newCapacity = growCapacity(requiredBytes)
        remap(newCapacity)
    }

    private fun growCapacity(requiredBytes: Long): Long {
        val current = dataCapacityBytes.coerceAtLeast(GROWTH_BLOCK_BYTES.toLong())
        var next = current

        while (next < requiredBytes) {
            next = max(next * 2, next + GROWTH_BLOCK_BYTES)
        }
        return next
    }

    private fun remap(newCapacityBytes: Long) {
        mapped.force()
        val newSize = header.pixelOffset + newCapacityBytes
        raf.setLength(newSize)
        mapped = channel.map(
            FileChannel.MapMode.READ_WRITE,
            header.pixelOffset,
            newCapacityBytes
        )
        dataCapacityBytes = newCapacityBytes
    }

    private fun indexWithOffset(x: Int, y: Int) =
        indexOf(x, y) * BYTES_PER_PIXEL + header.pixelOffset

    override fun close() {
        mapped.force()
        channel.close()
        raf.close()
        file.deleteRecursively()
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Simpler file logic

    private fun readHeader(): FileHeader {
        raf.seek(0)
        val headerLine = raf.readLine() ?: error("File is empty: ${file.absolutePath}")
        val pixelsOffset = raf.filePointer
        return FileHeader.parse(headerLine, pixelsOffset)
    }

    private fun writeHeader(newHeader: FileHeader) {
        raf.seek(0)
        raf.writeChars(newHeader.pack())
        raf.writeChars("\n")
    }

    private fun getBitmapHeight(): Long {
        val rowSize = raf.length() - header.pixelOffset
        val bytesPerPixel = BYTES_PER_PIXEL
        return rowSize / bytesPerPixel / width
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Implementation of the api

    override val width: Int
        get() = header.width

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

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y in 0 ..< height) { "y out of bounds: $y, height=${height - 1}" }

        val offset = indexWithOffset(x, y)
        return mapped.getInt(offset.toInt())
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y >= 0) { "y < 0: $y" }

        val requiredPixels = indexOf(x, y) + 1L
        ensureCapacityFor(requiredPixels)

        val offset = indexWithOffset(x, y)
        mapped.putInt(offset.toInt(), value)
    }

    override fun getCopy(): Pixels {
        val newFile = createFile()
        Files.copy(file.toPath(), newFile.toPath())
        return MappedRafBitmap(file, createFile)
    }

    companion object {
        fun writePixelsIntoStorage(
            file: File,
            header: FileHeader
        ) {
            file.deleteRecursively()
            file.createNewFile()
            val packed = header.pack()
            file.writer().use {
                it.write(packed, 0, packed.length)
            }
        }
    }
}