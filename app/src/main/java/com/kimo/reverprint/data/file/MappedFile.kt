package com.kimo.reverprint.data.file

import android.annotation.SuppressLint
import com.kimo.reverprint.tools.file.IFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files

class MappedFile private constructor(
    private val file: File,
    private val createNewFile: () -> File
): IFile {

    private val raf = RandomAccessFile(file, "rw")
    private val channel: FileChannel = raf.channel

    private val chunk = Chunk(
        0L, 1L,
        maxChunkSize = MAX_CHUNK_SIZE.toInt()
    )

    private var mapped: MappedByteBuffer = channel.map(
        FileChannel.MapMode.READ_WRITE,
        chunk.startPos, chunk.size
    )

    private fun forceMap() {
        val requiredEnd = chunk.startPos + chunk.size
        ensureCapacity(requiredEnd)
        mapped.force()
        mapped = channel.map(
            FileChannel.MapMode.READ_WRITE,
            chunk.startPos,
            chunk.size
        )
    }

    private fun ensureCapacity(required: Long) {
        if (raf.length() >= required) return
        raf.setLength(required)
    }

    override val size: Long get() = file.length()

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
        data.forEachIndexed { index, b ->
            setInMap(offset + index, b)
        }
    }

    override fun copy(): IFile {
        val newFile = createNewFile()
        @SuppressLint("NewApi") Files.copy(file.toPath(), newFile.toPath())
        return MappedFile(newFile, createNewFile)
    }


    // ACCESS TO MAP

    private fun setInMap(byteIndex: Long, int: Int) {
        chunk.moveToPoint(byteIndex, 0..Long.MAX_VALUE) {
            forceMap()
        }
        val index = byteIndex - chunk.startPos
        mapped.putInt(index.toInt(), int)
    }

    private fun setInMap(byteIndex: Long, b: Byte) {
        chunk.moveToPoint(byteIndex, 0..Long.MAX_VALUE) {
            forceMap()
        }
        val index = byteIndex - chunk.startPos
        mapped.put(index.toInt(), b)
    }

    private fun getFromMap(byteIndex: Long): Int {
        chunk.moveToPoint(byteIndex, 0..<raf.length()) {
            forceMap()
        }
        val index = byteIndex - chunk.startPos
        return mapped.getInt(index.toInt())
    }



    companion object {
        fun create(createNewFile: () -> File): MappedFile {
            return MappedFile(
                file = createNewFile(),
                createNewFile = createNewFile
            )
        }
        private const val MAX_CHUNK_SIZE = Int.MAX_VALUE.toLong()
    }
}


class Chunk(
    initStart: Long,
    initSize: Long,
    private val maxChunkSize: Int
) {
    var startPos = initStart
        private set
    var size = initSize
        private set

    fun moveToPoint(
        pointPosition: Long,
        bounds: LongRange,
        onMoved: (() -> Unit)? = null
    ) {
        if (bounds.isEmpty()) return

        val oldStart = startPos
        val oldSize = size

        val minStart = bounds.first
        val maxAllowedSize = minOf(
            maxChunkSize.toLong(),
            bounds.last - bounds.first + 1
        )

        fun clampStart(desiredStart: Long, chunkSize: Long): Long {
            val maxStart = bounds.last - chunkSize + 1
            return when {
                maxStart < minStart -> minStart
                desiredStart < minStart -> minStart
                desiredStart > maxStart -> maxStart
                else -> desiredStart
            }
        }

        val target = pointPosition.coerceIn(bounds.first, bounds.last)

        // Считаем конец чанка как "exclusive end"
        val endPos = startPos + size

        when {
            target > endPos -> {
                var need = target - endPos

                // Сначала расширяемся
                val grow = minOf(need, maxAllowedSize - size)
                size += grow
                need -= grow

                // Если уже упёрлись в максимум — двигаем чанк
                if (need > 0) {
                    startPos += need
                }
            }

            target < startPos -> {
                var need = startPos - target

                // Сначала расширяемся влево
                val grow = minOf(need, maxAllowedSize - size)
                startPos -= grow
                size += grow
                need -= grow

                // Если уже упёрлись в максимум — двигаем чанк
                if (need > 0) {
                    startPos -= need
                }
            }

            else -> return // точка уже внутри чанка
        }

        startPos = clampStart(startPos, size)

        if (oldSize != size || oldStart != startPos)
            onMoved?.invoke()
    }
}