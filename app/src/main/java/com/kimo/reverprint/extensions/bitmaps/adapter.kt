package com.kimo.reverprint.extensions.bitmaps

import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.forEach

private typealias DomainColorModel = com.kimo.reverprint.domain.images.ColorModel
private typealias ImplementedColorModel = com.kimo.reverprint.tools.graphics.ColorModel

suspend fun BitmapCreator.create(pixels: ImagePixels): Pixels {
    val copy = create(
        pixels.width,
        pixels.height,
        pixels.model.implementedEquivalent()
    )
    copy.forEach { p, x, y ->
        p[x, y] = Color(
            pixels.pixelList[y * pixels.width + x]
        )
    }
    return copy
}

fun Pixels.asDomainImmutable() =
    object : ImagePixels {

        override val pixelList: List<Int>
            get() = this@asDomainImmutable.pixelList

        override val width: Int
            get() = this@asDomainImmutable.width

        override val height: Int
            get() = this@asDomainImmutable.height

        override val model: DomainColorModel
            get() = colorModel.domainEquivalent()

        override fun row(y: Int): IntArray =
            IntArray(width) { x -> get(x, y).int }

    }


fun DomainColorModel.implementedEquivalent(): ImplementedColorModel {
    return when (this) {
        DomainColorModel.ARGB_8 -> Argb8
        DomainColorModel.GREY_4BPP -> Grey4
        DomainColorModel.MONO -> Monochrome
    }
}

fun ImplementedColorModel.domainEquivalent(): DomainColorModel {
    return when (this) {
        is Argb8 -> DomainColorModel.ARGB_8
        is Grey4 -> DomainColorModel.GREY_4BPP
        is Monochrome -> DomainColorModel.MONO
        is Grey8 -> error("Grey8bpp is not used in domain")
        else -> error("Color model (${this::class.simpleName}) is not used in domain")
    }
}