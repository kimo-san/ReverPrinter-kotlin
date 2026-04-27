package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import java.io.File

/**
 * Mark single fabric
 */
interface MonomodeFabric

class BitmapFabric(createFile: () -> File): BitmapCreator {

    val raf = RafBitmapCreator(createFile)
    val mapRaf = MappedRafBitmapCreator(createFile)
    val ram = RamBitmapCreator()

    override suspend fun create(
        width: Int,
        height: Int,
        colorModel: ColorModel
    ): Pixels = ram.create(width, height, colorModel)

    override suspend fun createExtendable(
        width: Int,
        colorModel: ColorModel
    ): Pixels = raf.create(width, 0, colorModel)

    override suspend fun createFastExtendable(
        width: Int,
        colorModel: ColorModel
    ): Pixels = mapRaf.create(width, colorModel)

}
