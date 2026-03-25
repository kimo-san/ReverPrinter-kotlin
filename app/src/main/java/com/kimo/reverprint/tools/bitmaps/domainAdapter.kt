package com.kimo.reverprint.tools.bitmaps

import com.kimo.reverprint.tools.bitmaps.impl.Argb8
import com.kimo.reverprint.tools.bitmaps.impl.Grey4
import com.kimo.reverprint.tools.bitmaps.impl.Grey8
import com.kimo.reverprint.tools.bitmaps.impl.Monochrome

typealias DomainColorModel = com.kimo.reverprint.domain.ColorModel

fun DomainColorModel.implementedEquivalent(): ColorModel {
    return when (this) {
        DomainColorModel.ARGB_8 -> Argb8
        DomainColorModel.GREY_4BPP -> Grey4
        DomainColorModel.MONO -> Monochrome
    }
}

fun ColorModel.domainEquivalent(): DomainColorModel {
    return when (this) {
        is Argb8 -> DomainColorModel.ARGB_8
        is Grey4 -> DomainColorModel.GREY_4BPP
        is Monochrome -> DomainColorModel.MONO
        is Grey8 -> error("Grey8bpp is not used in domain")
        else -> error("Current model is not used in domain")
    }
}