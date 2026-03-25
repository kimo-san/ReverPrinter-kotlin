package com.kimo.reverprint.useCases.tinyprint.units

import com.kimo.reverprint.domain.ColorModel
import com.kimo.reverprint.domain.DeviceController
import com.kimo.reverprint.domain.ImagePixels
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.useCases.tinyprint.DeviceChecker
import com.kimo.reverprint.useCases.tinyprint.DeviceCommunicationProtocol
import com.kimo.reverprint.useCases.tinyprint.PrintPreview
import com.kimo.reverprint.useCases.tinyprint.TinyprintDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class Printer(
    val getPairedDevice: () -> TinyprintDevice,
    val sendBytes: suspend (ByteArray) -> Unit,
    val getDeviceChecker: () -> DeviceChecker,
    val protocol: DeviceCommunicationProtocol
) {

    suspend fun print(
        imagePreview: DeviceController.PrintPreviews,
        mode: PrintMode
    ) = withContext(Dispatchers.Default) {

        require(imagePreview is PrintPreview)
        val device = getPairedDevice()
        val checker = getDeviceChecker()
        val bitmap = imagePreview.imagePixels[mode]

        require(bitmap != null)
        when (mode) {
            PrintMode.BPP4 -> print4bpp(checker, bitmap, device)
            PrintMode.BPP1 -> print1bpp(checker, bitmap)
        }

        if (imagePreview.appliedConfiguration.addSpaceAfterPrint)
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

        repeat(bitmap.height) { y ->
            checker.suspendIfOverloaded()
            print("Line $y (max: ${bitmap.height - 1})")
            sendBytes(protocol.println4bpp(bitmap.row(y), device.usesCompressionForGreyScale))
            println(" - sent")
            if (y % device.blockCountForGreyPrint == 0) {
                delay(device.grayImageSpeed.toLong())
            }
        }
    }

    private suspend fun print1bpp(
        checker: DeviceChecker,
        bitmap: ImagePixels
    ) = coroutineScope {

        require(bitmap.model == ColorModel.MONO)
        sendBytes(protocol.setMode(DeviceCommunicationProtocol.Mode.MONO_IMG))
        sendBytes(protocol.setQuality(DeviceCommunicationProtocol.Quality.Five))

        repeat(bitmap.height) { y ->
            checker.suspendIfOverloaded()
            println("Line $y (max: ${bitmap.height - 1})")
            sendBytes(protocol.println1bpp(bitmap.row(y)))
        }
    }
}