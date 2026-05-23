package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.Pixels

class BitmapFabric(
    private vararg val creators: BitmapCreator
): BitmapCreator {

    override suspend fun create(config: BitmapConfig): Pixels {
        return creators.find { it.canApply(config) }
            ?.create(config)
            ?: error("Invalid config. Cannot apply it to creator.")
    }

    override fun canApply(config: BitmapConfig): Boolean {
        return creators.any { it.canApply(config) }
    }
}