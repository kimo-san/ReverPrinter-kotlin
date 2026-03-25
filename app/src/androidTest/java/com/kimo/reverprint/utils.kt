package com.kimo.reverprint

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kimo.reverprint.androidData.AndroidBluetoothLeController
import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.tools.bitmaps.MutablePixels
import com.kimo.reverprint.useCases.tinyprint.TinyprintController
import com.kimo.reverprint.useCases.tinyprint.units.ProtocolImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.runner.RunWith

/**
 * Tests under this class require a real, turned-on printer nearby.
 * Mainly, inherited tests should not use virtual time like `delay()` in runTest of coroutines,
 * because it is useless here.
 */
@RunWith(AndroidJUnit4::class)
abstract class TinyprintTestWrapper {
    companion object {

        private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val protocol = ProtocolImpl()
        val bluetoothController = AndroidBluetoothLeController(context)
        val controller: TinyprintController = TinyprintController(bluetoothController, protocol)

        val testBitmap get() = createTestBitmap(100)

        @JvmStatic
        @BeforeClass
        fun setup() {
            runBlocking {
                controller.findAvailable().first()
                    .also { controller.connect(it) }
            }
        }

        @JvmStatic
        @ClassRule
        fun permissionRule(): GrantPermissionRule {
            val permissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) else arrayOf(
                    Manifest.permission.BLUETOOTH
                )
            return GrantPermissionRule.grant(*permissions)
        }

        @JvmStatic
        @AfterClass
        fun releaseConnection() = runBlocking {
            controller.disconnect()
        }
    }
}


@Suppress("SameParameterValue")
private fun createTestBitmap(size: Int): ImagePixels = runBlocking {

    val pixels = MutablePixels(
        pixelsArray = IntArray(size * size),
        width = size,
        height = size,
        model = ColorModel.ARGB_8
    )

    pixels.forEach { p, x, y ->
        val isBlack = (x + y) % 2 == 0
        p[x, y] = if (isBlack) Color.BLACK else Color.WHITE
    }

    pixels
}