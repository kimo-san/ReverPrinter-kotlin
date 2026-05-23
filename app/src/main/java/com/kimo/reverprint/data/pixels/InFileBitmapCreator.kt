package com.kimo.reverprint.data.pixels

import com.kimo.reverprint.tools.file.FileCreator
import com.kimo.reverprint.tools.file.FileTypes
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InFileBitmapCreator(
    val fileCreator: FileCreator
): BitmapCreator {

    private suspend fun create(
        width: Int,
        colorModel: ColorModel
    ): Pixels = withContext(Dispatchers.IO) {
        val file = fileCreator.create(FileTypes.MAPPED)
        InFileBitmap.writePixelsIntoStorage(
            file,
            FileHeader.from(width, colorModel)
        )
        InFileBitmap(file)
    }

    override suspend fun create(config: BitmapConfig): Pixels {
        return create(config.width, config.colorModel)
    }

    override fun canApply(config: BitmapConfig): Boolean {
        return config.storage == StorageType.MAPPED_FILE
    }
}