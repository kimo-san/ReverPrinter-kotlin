package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType.*
import java.io.File

/**
 * Mark single fabric
 */
interface MonomodeFabric

class BitmapFabric(
    createFile: () -> File
): BitmapCreator {

    val ram = RamBitmapCreator()
    val raf = RafBitmapCreator(createFile)
    val mapRaf = MappedRafBitmapCreator(createFile)

    override suspend fun create(config: BitmapConfig): Pixels {
        return when (config.storage) {
            RAM -> ram.create(config.width, config.height ?: error("For StorageType.RAM must be height not null"), config.colorModel)
            RAF -> raf.create(config.width, config.colorModel)
            MAPPED_RAF -> mapRaf.create(config.width, config.colorModel)
        }
    }

}
