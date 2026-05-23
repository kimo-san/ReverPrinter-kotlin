package com.kimo.reverprint

import com.kimo.reverprint.data.file.Chunk
import org.junit.Test

class FileTest {

    operator fun String.times(other: Long): String {
        return buildString {
            repeat(other.toInt()) {
                append(this@times)
            }
        }
    }

    fun Chunk.stringState(): String {
        return "Chunk(${"-" * startPos}${"X" * size})"
    }

    @Test
    fun chunking() {
        val chunk = Chunk(
            initStart = 0,
            initSize = 1,
            maxChunkSize = 5
        )

        chunk.moveToPoint(0, bounds = 0..10L) { println("Moved!") }
        println(chunk.stringState())

        chunk.moveToPoint(1, bounds = 0..10L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(2, bounds = 0..10L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(5, bounds = 0..11L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(7, bounds = 0..12L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(0, bounds = 0..12L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(12, bounds = 0..12L) { println("Moved!") }
        println(chunk.stringState())
        chunk.moveToPoint(11, bounds = 0..12L) { println("Moved!") }
        println(chunk.stringState())
    }
}