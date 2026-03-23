package com.kimo.reverprint

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kimo.reverprint.data.bluetooth.AndroidBluetoothLeController
import com.kimo.reverprint.data.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.data.tinyprint.TinyprintController
import com.kimo.reverprint.di.initializeTinyprintController
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration

/**
 * These following tests require a real, turned-on device nearby.
 */
@RunWith(AndroidJUnit4::class)
class RealTinyprintDeviceTest {

    lateinit var context: Context

    @Before
    fun setupVars() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    @Rule
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

    @Test
    fun ableToUseBluetooth() {
        context.getSystemService(BluetoothManager::class.java)
            .adapter.also { assert(it != null) }
    }

    @Test
    fun testDeviceDiscovery() = runBlocking {
        val controller = AndroidBluetoothLeController(context, null, null)

        var foundSomething = false
        val discovery = launch {
            controller.discover().collect { foundSomething = true }
        }
        delay(Duration.ofMillis(2000))

        discovery.cancelAndJoin()
        assert(foundSomething)
    }

    suspend fun withTinyprint(l: suspend (controller: TinyprintController) -> Unit) =
        withTimeout(10_000) {
            val controller = initializeTinyprintController(context)
            val device = withTimeout(Duration.ofMillis(5000)) {
                controller.findAvailable().first()
            }
            controller.connect(device)
            l(controller)
            controller.disconnect()
        }

    @Test
    fun testTinyprintController() = runBlocking { withTinyprint { } }

    @Test
    fun testTinyprintInput() = runBlocking {
        withTinyprint { controller ->
            controller.bluetoothController.send(
                controller.protocol.feedPaper(10)
            )
            delay(100)
            controller.bluetoothController.send(
                controller.protocol.retractPaper(10)
            )
        }
    }

    @Test
    fun testTinyprintIO() = runBlocking {
        withTinyprint { controller ->

            var anyMessage: Any? = null
            val job = controller.bluetoothController.read()
                .map { controller.protocol.parseReceivedMessage(it) }
                .filter { it is DeviceCommunicationProtocol.DeviceAnswer.State }
                .map { it as DeviceCommunicationProtocol.DeviceAnswer.State }
                .onEach { println("Parsed: ${it.warning()}, ${it.batteryLevel()}") }
                .onEach { anyMessage = it }
                .launchIn(this)
            delay(500)

            controller.bluetoothController.send(
                controller.protocol.returnState()
            )
            println("wait...")
            delay(1000)
            println("... finish")
            job.cancelAndJoin()

            assert(anyMessage != null)
        }
    }


    @Test
    fun testProtocol() = runBlocking {
        withTinyprint { controller ->
            buildList {

                add(intArrayOf(
                    0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x00, 0x04, 0x24, 0xa8, 0xff
                ))
                add(intArrayOf(
                    0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x01, 0x1b, 0x28, 0x73, 0xff
                ))

            }.map { it.map { it.toByte() }.toByteArray() }.forEach {

                it.let(controller.protocol::parseReceivedMessage)
                    .let { it as DeviceCommunicationProtocol.DeviceAnswer.State }
                    .also { println("Parsed: ${it.warning()}, ${it.batteryLevel()}") }
            }
        }
    }
}