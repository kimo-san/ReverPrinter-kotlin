package com.kimo.reverprint.data.file

import android.annotation.SuppressLint
import com.kimo.reverprint.tools.file.IFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.math.max

class MappedFile(
    private val file: File,
    initialLogicalSize: Long,
    private val createNewFile: () -> File
): IFile {

    private val raf = RandomAccessFile(file, "rw")
    private val channel: FileChannel = raf.channel

    private var currentChunkStart = 0L

    private var logicalSize = initialLogicalSize

    override val size: Long get() = logicalSize

    override fun clean() {
        file.delete()
        file.createNewFile()
    }

    override fun writeInt(byteOffset: Long, value: Int) {
        setInMap(byteOffset, value)
    }

    override fun readInt(byteIndex: Long): Int {
        return getFromMap(byteIndex)
    }

    override fun readFirstLine(): String? {
        raf.seek(0)
        return raf.readLine()
    }
    override fun readFirstLineByteSize(): Long {
        raf.seek(0)
        raf.readLine()
        return raf.filePointer
    }

    override fun writeBytes(offset: Long, data: ByteArray) {
        raf.seek(offset)
        raf.write(data)
    }

    override fun copy(): IFile {
        val newFile = createNewFile()

        @SuppressLint("NewApi") // todo
        Files.copy(file.toPath(), newFile.toPath())

        return MappedFile(newFile, logicalSize, createNewFile)
    }

    private var mapped: MappedByteBuffer = channel.map(
        FileChannel.MapMode.READ_WRITE,
        0, CHUNK_SIZE
    )

    private fun setInMap(byteIndex: Long, int: Int) {
        checkChunkFor(byteIndex)
        val index = byteIndex - currentChunkStart
        mapped.putInt(index.toInt(), int)
    }

    private fun getFromMap(byteIndex: Long): Int {
        checkChunkFor(byteIndex)
        val index = byteIndex - currentChunkStart
        return mapped.getInt(index.toInt())
    }

    private fun checkChunkFor(byteIndex: Long) {

        logicalSize = max(byteIndex, logicalSize)
        val newChunkStart =
            if (byteIndex < currentChunkStart)
                max(0, currentChunkStart - CHUNK_SIZE)
            else if (byteIndex > CHUNK_SIZE + currentChunkStart)
                max(0, currentChunkStart + CHUNK_SIZE)
            else return


        currentChunkStart = newChunkStart
        mapped.force()

        println("offset: $newChunkStart")
        mapped = channel.map(
            FileChannel.MapMode.READ_WRITE,
            newChunkStart,
            CHUNK_SIZE
        )
    }

    companion object {
        private const val CHUNK_SIZE = Int.MAX_VALUE.toLong()
    }
}