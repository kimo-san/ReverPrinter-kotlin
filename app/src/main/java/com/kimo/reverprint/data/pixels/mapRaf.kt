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
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.math.max
import kotlin.math.min

private const val GROWTH_BLOCK_BYTES = 1024 * 1024
private const val BYTES_PER_PIXEL = Int.SIZE_BYTES
private const val MAX_CHUNK_SIZE = Int.MAX_VALUE.toLong()

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
            FileHeader.from(width, colorModel)
        )
        MappedRafBitmap(file, createFile)
    }
}


private class MappedRafBitmap(
    private val file: File,
    private val createFile: () -> File
) : AbstractPixels(), CloseablePixels {

    override val storageType: StorageType = StorageType.MAPPED_RAF

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Raf + Channel logic

    private val raf = RandomAccessFile(file, "rw")
    private val channel: FileChannel = raf.channel
    private var header: FileHeader = readHeader()
    private var mapped: MappedByteBuffer = mapData(header.pixelOffset)

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


    var usedPixelsCount = (file.length() - header.pixelOffset) / BYTES_PER_PIXEL
    var currentChunkStart = 0L
    var currentChunkEnd = 0L

    private fun setInMap(index: Long, int: Int) {
        checkChunkForIndex(index)
        val index = index - currentChunkStart
        mapped.putInt(index.toInt(), int)
    }

    private fun getFromMap(index: Long): Int {
        checkChunkForIndex(index)
        val index = index - currentChunkStart
        return mapped.getInt(index.toInt())
    }

    private fun checkChunkForIndex(pixelIndex: Long) {
        usedPixelsCount = max(pixelIndex / BYTES_PER_PIXEL, usedPixelsCount)

        val newChunkStart =
            if (pixelIndex < currentChunkStart)
                max(0, currentChunkStart - MAX_CHUNK_SIZE)
            else if (pixelIndex > currentChunkEnd)
                max(0, currentChunkStart + MAX_CHUNK_SIZE)
            else return

        val pixelByteSize = file.length() - header.pixelOffset
        val newChunkEnd = min(pixelByteSize, newChunkStart + BYTES_PER_PIXEL)

        mapped = channel.map(
            FileChannel.MapMode.READ_WRITE,
            header.pixelOffset,
            newChunkEnd - newChunkStart
        )
    }


    override fun close() {
        mapped.force()
        mapped.clear()
        channel.close()
        raf.close()
        file.delete()
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Simpler file logic

    private fun indexInFile(x: Int, y: Int) =
        indexOf(x, y) * BYTES_PER_PIXEL

    private fun readHeader(): FileHeader {
        raf.seek(0)
        val headerLine = raf.readLine() ?: error("File is empty: ${file.absolutePath}")
        val pixelsOffset = raf.filePointer
        return FileHeader.parse(headerLine, pixelsOffset)
    }

    private fun writeHeader(newHeader: FileHeader) {
        raf.seek(0)
        raf.write(newHeader.pack())
    }

    private fun getBitmapHeight(): Int {
        return (usedPixelsCount / header.width).toInt().coerceAtLeast(1)
    }

    // - - - - - - - - - - - - - - - - - - - - - - -
    // Implementation of the api

    override val width: Int
        get() = header.width

    override val height: Int
        get() = getBitmapHeight()

    override var colorModel: ColorModel
        get() = header.colorModel
        set(value) {
            writeHeader(header.withRewrittenColorModel(value))
        }

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y in 0 ..< height) { "y out of bounds: $y, height=${height - 1}" }
        return getFromMap(indexInFile(x, y))
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        require(x in 0 ..< width) { "x out of bounds: $x, max: ${width - 1}" }
        require(y >= 0) { "y < 0: $y" }

        setInMap(indexInFile(x, y), value)
    }

    override fun getCopy(): Pixels {
        val newFile = createFile()
        Files.copy(file.toPath(), newFile.toPath())
        return MappedRafBitmap(newFile, createFile)
    }

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