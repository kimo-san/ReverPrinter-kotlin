package com.kimo.reverprint

import com.kimo.reverprint.data.file.MappedFile
import org.junit.Test
import java.io.File

class FileTest {

    val context = getContext()

    fun createFile(): File {
        val file = File(context.filesDir, "test-file-${Math.random().toInt()}.bin")
        file.createNewFile()
        return file
    }


    @Test
    fun doesNotOverlapSize() {

        val file = createFile()
        val given = MappedFile.create {
            file
        }

        println("Size since just created: " + given.size)

        val toAdd = byteArrayOf(1, 0, 1)
        given.writeBytes(0, toAdd)

        println("Size since added ${toAdd.size} bytes at once: " + given.size)

        println(file.readBytes().toHexString())
        given.writeInt(1, 0)

        println(file.readBytes().toHexString())
    }








}