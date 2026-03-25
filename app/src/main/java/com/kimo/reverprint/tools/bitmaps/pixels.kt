package com.kimo.reverprint.tools.bitmaps

import com.kimo.reverprint.domain.ImagePixels
import kotlinx.coroutines.yield
import kotlin.math.min

class ImmutablePixels(mutablePixels: MutablePixels): ImagePixels by mutablePixels

class MutablePixels(
    private val pixelsArray: IntArray,
    override val width: Int,
    override val height: Int,
    var implementedModel: ColorModel
): ImagePixels {

    override val model: DomainColorModel
        get() = implementedModel.domainEquivalent()

    override val pixelList: List<Int> get() = pixelsArray.asList()
    fun copy() = MutablePixels(pixelsArray.copyOf(), width, height, implementedModel)

    private val maxIndex = width * height - 1
    fun getIndexForPixel(x: Int, y: Int) = min(
        y * width + min(x, width - 1),
        maxIndex
    )

    operator fun get(x: Int, y: Int): Int {
        return pixelsArray[getIndexForPixel(x, y)]
    }

    operator fun set(x: Int, y: Int, value: Int) {
        pixelsArray[getIndexForPixel(x, y)] = value
    }

    override fun row(y: Int): IntArray = IntArray(width) { x -> get(x, y) }

    suspend fun forEach(block: (p: MutablePixels, x: Int, y: Int) -> Unit) {
        repeat(height) { y ->
            yield()
            repeat(width) { x ->
                block(this, x, y)
            }
        }
    }
}