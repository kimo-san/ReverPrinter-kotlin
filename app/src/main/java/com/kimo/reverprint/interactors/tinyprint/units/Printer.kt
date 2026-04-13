package com.kimo.reverprint.interactors.tinyprint.units

import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.interactors.tinyprint.DeviceChecker
import com.kimo.reverprint.interactors.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.interactors.tinyprint.PrintPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Printer(
    val deviceGetter: () -> TinyprintDevice,
    val sendBytes: suspend (ByteArray) -> Unit,
    val deviceChecker: () -> DeviceChecker,
    val protocol: DeviceCommunicationProtocol
) {

    suspend fun print(
        imagePreview: DeviceController.PrintPreviews,
        mode: PrintMode
    ) = withContext(Dispatchers.Default) {

        require(imagePreview is PrintPreview)
        val device = deviceGetter()
        val checker = deviceChecker()
        val bitmap = imagePreview.imagePixels[mode]

        require(bitmap != null)
        when (mode) {
            PrintMode.BPP4 -> print4bpp(checker, bitmap, device)
            PrintMode.BPP1 -> print1bpp(checker, bitmap)
        }

        if (imagePreview.appliedPrintConfig.addSpaceAfterPrint)
            sendBytes(protocol.feedPaper(120))
    }

    private suspend fun print4bpp(
        checker: DeviceChecker,
        bitmap: ImagePixels,
        device: TinyprintDevice
    ) = coroutineScope {

        require(bitmap.model == ColorModel.GREY_4BPP)
        sendBytes(protocol.setMode(DeviceCommunicationProtocol.Mode.GREY_IMG))
        sendBytes(protocol.setQuality(DeviceCommunicationProtocol.Quality.Five))
        sendBytes(protocol.setEnergy(1))

        var loseCounter = 0
        repeat(bitmap.height) { y ->
            checker.suspendIfNeeded()
            print("Line $y (max: ${bitmap.height - 1})")
            loseOnTimeout(
                durationInMillis = 50,
                onLose = { println("...lost!"); loseCounter++ }
            ) {
                sendBytes(protocol.println4bpp(bitmap.row(y), device.usesCompressionForGreyScale))
                println("...sent!")
            }
            if (y % device.blockCountForGreyPrint == 0) {
                delay(device.grayImageSpeed.toLong())
            }
        }
        println("Lost: $loseCounter of ${bitmap.height}")
    }

    private suspend fun print1bpp(
        checker: DeviceChecker,
        bitmap: ImagePixels
    ) = coroutineScope {

        require(bitmap.model == ColorModel.MONO)
        sendBytes(protocol.setMode(DeviceCommunicationProtocol.Mode.MONO_IMG))
        sendBytes(protocol.setQuality(DeviceCommunicationProtocol.Quality.Five))

        var loseCounter = 0
        repeat(bitmap.height) { y ->
            checker.suspendIfNeeded()
            print("Line $y (max: ${bitmap.height - 1})...")

            loseOnTimeout(
                durationInMillis = 50,
                onLose = { println("...lost!"); loseCounter++ }
            ) {
                sendBytes(protocol.println1bpp(bitmap.row(y)))
                println("...done!")
            }
        }
        println("Lost: $loseCounter of ${bitmap.height}")
    }

    private suspend fun loseOnTimeout(
        durationInMillis: Long,
        onLose: () -> Unit,
        todo: suspend () -> Unit
    ) = coroutineScope {
        val worker = launch {
            todo()
        }
        val watchdog = launch {
            delay(durationInMillis)
            worker.cancelAndJoin()
            onLose()
        }
        worker.join()
        watchdog.cancelAndJoin()
    }
}