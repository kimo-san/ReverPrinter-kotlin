package com.kimo.reverprint.tools.file

interface IFile {
    val size: Long
    fun clean()
    fun writeInt(byteOffset: Long, value: Int)
    fun readInt(byteIndex: Long): Int
    fun readFirstLine(): String?
    fun readFirstLineByteSize(): Long
    fun writeBytes(offset: Long, data: ByteArray)
    fun copy(): IFile
}