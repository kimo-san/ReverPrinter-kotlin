package com.kimo.reverprint

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kimo.reverprint.android.data.AndroidBluetoothLeController
import com.kimo.reverprint.data.pixels.BitmapFabric
import com.kimo.reverprint.extensions.bitmaps.BitmapConverterImpl
import com.kimo.reverprint.providers.tinyprint.PreviewGenerator
import com.kimo.reverprint.providers.tinyprint.TinyprintBluetoothController
import com.kimo.reverprint.providers.tinyprint.TinyprintManager
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
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
        val bluetoothController: BluetoothLeController = AndroidBluetoothLeController(context)
        val controller: TinyprintManager = TinyprintManager(
            TinyprintBluetoothController(bluetoothController),
            previewGenerator = PreviewGenerator(
                converter = BitmapConverterImpl(BitmapFabric({ error("Slop") }))
            )
        )

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