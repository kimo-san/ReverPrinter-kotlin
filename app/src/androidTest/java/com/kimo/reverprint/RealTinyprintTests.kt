package com.kimo.reverprint

import com.kimo.reverprint.useCases.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.domain.DeviceController
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
    fun testDeviceDiscovery(): Unit = runTest {
        controller.bluetoothController.discovery().first()
    }

    @Test
    fun testTinyprintInput() = runTest {
        controller.bluetoothController.send(
            controller.protocol.feedPaper(10)
        )
        controller.bluetoothController.send(
            controller.protocol.retractPaper(10)
        )
    }

    @Test
    fun testIO() = runTest {

        val command = controller.protocol.requestState()
        val expectedMessageType = DeviceCommunicationProtocol.DeviceAnswer.State::class

        controller.bluetoothController.send(command)
        val answer: DeviceCommunicationProtocol.DeviceAnswer = controller.bluetoothController.read()
            .map { controller.protocol.parseReceivedMessage(it) }
            .filterNotNull()
            .onEach { println("Parsed: $it") }
            .first()

        assert(expectedMessageType.isInstance(answer))
    }

    @Test
    fun testProtocolOn_x6h_model(): Unit = runTest {

        val (data1, exp1) = intArrayOf(
            0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x00, 0x04, 0x24, 0xa8, 0xff
        ).map { it.toByte() }.toByteArray() to 2f / 6
        val (data2, exp2) = intArrayOf(
            0x51, 0x78, 0xa3, 0x01, 0x03, 0x00, 0x01, 0x1b, 0x28, 0x73, 0xff
        ).map { it.toByte() }.toByteArray() to 1f

        data1.let(controller.protocol::parseReceivedMessage)
            .let { it as DeviceCommunicationProtocol.DeviceAnswer.State }
            .also { assert(it.batteryLevel() == exp1) }

        data2.let(controller.protocol::parseReceivedMessage)
            .let { it as DeviceCommunicationProtocol.DeviceAnswer.State }
            .also { assert(it.batteryLevel() == exp2) }
    }

    @Test
    fun print4bppPicture(): Unit = runBlocking {

        val readJob = controller.bluetoothController.read()
            .onEach { println("READ MESSAGE IN TEST: ${it.toHexString()}") }
            .launchIn(this)

        val previews = controller.generatePreviews(
            imageBitmap = testBitmap,
            configuration = DeviceController.Configuration(
                addSpaceAfterPrint = true,
                ditherImage = true
            )
        )
        controller.print(previews, PrintMode.BPP4)
        readJob.cancelAndJoin()
    }

    @Test
    fun print1bppPicture(): Unit = runBlocking {

        val readJob = controller.bluetoothController.read()
            .onEach { println("READ MESSAGE IN TEST: ${it.toHexString()}") }
            .launchIn(this)

        val previews = controller.generatePreviews(
            imageBitmap = testBitmap,
            configuration = DeviceController.Configuration(
                addSpaceAfterPrint = true,
                ditherImage = true
            )
        )
        controller.print(previews, PrintMode.BPP1)
        readJob.cancelAndJoin()
    }
}