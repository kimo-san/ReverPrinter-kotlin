package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType

class RamBitmapCreator: BitmapCreator {

    fun create(
        width: Int,
        height: Int,
        colorModel: ColorModel
    ): Pixels =  RamBitmap(width, height, colorModel, IntArray(width * height))

    override suspend fun create(config: BitmapConfig): Pixels {
        return create(config.width, config.height ?: error("For StorageType.RAM must be height not null"), config.colorModel)
    }

    override fun canApply(config: BitmapConfig): Boolean {
        return config.storage == StorageType.RAM &&
                config.height != null
    }
}