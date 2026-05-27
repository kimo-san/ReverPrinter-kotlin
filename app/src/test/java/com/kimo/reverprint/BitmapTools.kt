package com.kimo.reverprint

import com.kimo.reverprint.ForBitmapTests.createGradientBitmap
import com.kimo.reverprint.data.pixels.RamBitmapCreator
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.extensions.bitmaps.asDomainImmutable
import com.kimo.reverprint.extensions.bitmaps.from
import com.kimo.reverprint.extensions.bitmaps.implementedEquivalent
import com.kimo.reverprint.tools.graphics.Argb8
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Color
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Grey4
import com.kimo.reverprint.tools.graphics.Grey8
import com.kimo.reverprint.tools.graphics.Monochrome
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType
import com.kimo.reverprint.tools.graphics.fullFill
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

typealias DomainColorModel = com.kimo.reverprint.domain.images.ColorModel
const val BITMAP_SIZE = 10
const val TEST_CASE_COUNT = 1000

fun generateRandomColor(model: ColorModel) =
    model.colorOf((Math.random() * Int.MAX_VALUE).roundToInt())

fun randomColorModel(): ColorModel =
    colorModels.random()

val Color.hex get() = int.toHexString(HexFormat.UpperCase)

fun mostRandomBitmaps(): List<ImagePixels> = buildList {

    add(gradient.asDomainImmutable())

    repeat(TEST_CASE_COUNT) {
        val bp: ImagePixels = RandomBitmap()
        add(bp)
    }
}

fun mostRandomColors(): List<Pair<ColorModel, Color>> = buildList {
    repeat(TEST_CASE_COUNT) {
        val colorModel = randomColorModel()
        add(colorModel to generateRandomColor(colorModel))
    }
}


val creator: BitmapCreator = RamBitmapCreator()
val filled: Pixels =
    runBlocking {
        creator.create(
            BitmapConfig(
                BITMAP_SIZE, BITMAP_SIZE, Argb8,
                StorageType.RAM
            )
        ).apply {
            fullFill(generateRandomColor(colorModel))
        }
    }
    get() = field.getCopy()

val gradient: Pixels =
    runBlocking { creator.from(createGradientBitmap(BITMAP_SIZE, BITMAP_SIZE)) }
    get() = field.getCopy()

class RandomBitmap(
    override val width: Int = (Math.random() * 0xff).toInt(),
    override val height: Int = (Math.random() * 0xff).toInt(),
    override val model: DomainColorModel = DomainColorModel.entries.random()
) : ImagePixels {
    private val impl = model.implementedEquivalent()
    override val pixelList: List<Int> = buildList {
        repeat(width * height) {
            add(generateRandomColor(impl).int)
        }
    }
}

private val colorModels = listOf(
    Argb8,
    Grey8,
    Grey4,
    Monochrome
)