package com.kimo.reverprint

import com.kimo.reverprint.providers.tinyprint.DeviceProtocol
import com.kimo.reverprint.domain.DeviceManager
import com.kimo.reverprint.domain.PrintMode
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RealTinyprintTests : TinyprintTestWrapper() {

    @Test
    fun findsDevice(): Unit = runTest { controller.findAvailable().first() }

    @Test
    fun testDeviceDiscovery(): Unit = runBlocking {
        bluetoothController.discovery().first()
    }

    @Test
    fun testEnergyRegulation() {
        controller.protocol.setEnergy(7).also {
            println(it.toHexString())
        }
    }

    @Test
    fun testBasicIO() = runBlocking {

        controller.deviceController.stream {
            send(
                controller.protocol.feedPaper(10)
            )
            send(
                controller.protocol.retractPaper(10)
            )
        }
    }

    @Test
    fun testIO() = runBlocking {

        val command = controller.protocol.requestState()
        val expectedMessageType = DeviceProtocol.DeviceAnswer.State::class

        controller.deviceController.stream {
            send(command)
            val answer: DeviceProtocol.DeviceAnswer = read()
                .map { controller.protocol.parseReceivedMessage(it) }
                .filterNotNull()
                .onEach { println("Parsed upper level: $it") }
                .first()
            assert(expectedMessageType.isInstance(answer))
            println("Assertions succeed")
        }
        println(".. end exited")


    }

    @Test
    fun testProtocolOn_x6h_model(): Unit = runBlocking {

        val (data1, exp1) = intArrayOf(
            0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x00, 0x04, 0x24, 0xa8, 0xff
        ).map { it.toByte() }.toByteArray() to 2f / 6
        val (data2, exp2) = intArrayOf(
            0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x01, 0x1b, 0x28, 0x73, 0xff
        ).map { it.toByte() }.toByteArray() to 1f

        data1.let(controller.protocol::parseReceivedMessage)
            .let { it as DeviceProtocol.DeviceAnswer.State }
            .also { assert(it.batteryLevel() == exp1) }

        data2.let(controller.protocol::parseReceivedMessage)
            .let { it as DeviceProtocol.DeviceAnswer.State }
            .also { assert(it.batteryLevel() == exp2) }
    }

    @Test
    fun print4bppPicture(): Unit = runBlocking {

        val readJob = bluetoothController.read()
            .onEach { println("READ MESSAGE IN TEST: ${it.toHexString()}") }
            .launchIn(this)

        val previews = controller.generatePreviews(
            imageBitmap = createGradientBitmap(100, 20),
            printConfig = DeviceManager.PrintConfig(
                addSpaceAfterPrint = false,
                ditherImage = false
            )
        )
        controller.print(previews, PrintMode.BPP4)
        readJob.cancelAndJoin()
    }

    @Test
    fun print1bppPicture(): Unit = runBlocking {

        val readJob = bluetoothController.read()
            .onEach { println("READ MESSAGE IN TEST: ${it.toHexString()}") }
            .launchIn(this)

        val previews = controller.generatePreviews(
            imageBitmap = createChessBitmap(100, 20, 2),
            printConfig = DeviceManager.PrintConfig(
                addSpaceAfterPrint = true,
                ditherImage = true
            )
        )
        controller.print(previews, PrintMode.BPP1)
        readJob.cancelAndJoin()
    }
}