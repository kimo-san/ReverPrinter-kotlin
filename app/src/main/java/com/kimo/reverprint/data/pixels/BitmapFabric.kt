package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType

class BitmapFabric(
    vararg creators: BitmapCreator
): BitmapCreator {

    private val creators = creators.asList()
    override suspend fun create(config: BitmapConfig): Pixels {

        return creators.find { it.canApply(config) }
            ?.create(config)
            ?: error("Invalid config. Cannot apply it to creator.")
    }

    override fun canApply(config: BitmapConfig): Boolean {
        return creators.any { it.canApply(config) }
    }
}