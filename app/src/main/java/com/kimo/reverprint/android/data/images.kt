package com.kimo.reverprint.android.data

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.kimo.reverprint.tools.graphics.AbstractPixels
import com.kimo.reverprint.tools.graphics.BitmapConfig
import com.kimo.reverprint.tools.graphics.BitmapCreator
import com.kimo.reverprint.tools.graphics.ColorModel
import com.kimo.reverprint.tools.graphics.Pixels
import com.kimo.reverprint.tools.graphics.StorageType

class AndroidBitmapCreator: BitmapCreator {

    override suspend fun create(config: BitmapConfig): Pixels {
        val bp = createBitmap(
            config.width,
            config.height!!,
            Bitmap.Config.ARGB_8888
        )
        return AndroidPixels(bp, config.colorModel)
    }

    override fun canApply(config: BitmapConfig): Boolean {
        return config.height != null &&
                config.storage != StorageType.FILE
    }

}

class AndroidPixels(
    private val bitmap: Bitmap,
    initialColorModel: ColorModel
): AbstractPixels() {

    override val width: Int
        get() = bitmap.width
    override val height: Int
        get() = bitmap.height
    override var colorModel: ColorModel = initialColorModel
    override val storageType: StorageType
        get() = StorageType.NATIVE

    override fun getIntColorForPixel(x: Int, y: Int): Int {
        return bitmap[x, y]
    }

    override fun setIntColorForPixel(x: Int, y: Int, value: Int) {
        bitmap[x, y] = value
    }

    override fun getCopy(): Pixels {
        return AndroidPixels(Bitmap.createBitmap(bitmap), colorModel)
    }

}