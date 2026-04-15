package com.kimo.reverprint.interactors.bitmaps

import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.Pixels

private typealias DomainColorModel = com.kimo.reverprint.domain.ColorModel
private typealias ImplementedColorModel = com.kimo.reverprint.tools.graphics.ColorModel

fun Pixels(
    pixels: ImagePixels
): Pixels {
    val model = pixels.model.implementedEquivalent()
    return Pixels(
        pixels.width,
        pixels.height,
        model,
        IntArray(pixels.height * pixels.width) {
            model.colorOf(pixels.pixelList[it]).int
        },
    )
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