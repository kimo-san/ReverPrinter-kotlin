package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.images.ColorModel
import com.kimo.reverprint.domain.printer.DeviceManager
import com.kimo.reverprint.domain.images.ImagePixels
import com.kimo.reverprint.domain.printer.PrintMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun interface ByteSender {
    suspend fun send(bytes: ByteArray)
}

class TinyprintPrinter(
    val sender: ByteSender,
    val protocol: DeviceProtocol
) {

    suspend fun print(
        imagePreview: DeviceManager.PrintPreviews,
        mode: PrintMode
    ) = withContext(Dispatchers.Default) {

        require(imagePreview is PrintPreview)
        val bitmap = imagePreview.printable

        when (mode) {
            PrintMode.BPP4 -> print4bpp(bitmap)
            PrintMode.BPP1 -> print1bpp(bitmap)
        }

        if (imagePreview.appliedPrintConfig.addSpaceAfterPrint)
            sender.send(protocol.feedPaper(120))
    }

    private suspend fun print4bpp(
        bitmap: ImagePixels
    ) = coroutineScope {

        require(bitmap.model == ColorModel.GREY_4)
        sender.send(protocol.setMode(DeviceProtocol.Mode.GREY_IMG))
        sender.send(protocol.setQuality(DeviceProtocol.Quality.Five))
        sender.send(protocol.setEnergy(1))

        var loseCounter = 0
        repeat(bitmap.height) { y ->
            print("Line $y (max: ${bitmap.height - 1})")
            withLoseOnTimeout(
                durationInMillis = 50,
                onLose = { println("...lost!"); loseCounter++ }
            ) {
                sender.send(
                    protocol.println4bpp(
                        bitmap.row(y),
                        protocol.device.usesCompressionForGreyScale
                    )
                )
                println("...sent!")
            }
            if (y % protocol.device.blockCountForGreyPrint == 0) {
                delay(protocol.device.grayImageSpeed.toLong())
            }
        }
        println("Lost: $loseCounter of ${bitmap.height}")
    }

    private suspend fun print1bpp(
        bitmap: ImagePixels
    ) = coroutineScope {

        require(bitmap.model == ColorModel.MONO)
        sender.send(protocol.setMode(DeviceProtocol.Mode.MONO_IMG))
        sender.send(protocol.setQuality(DeviceProtocol.Quality.Five))

        var loseCounter = 0
        repeat(bitmap.height) { y ->
            print("Line $y (max: ${bitmap.height - 1})...")
            withLoseOnTimeout(
                durationInMillis = 50,
                onLose = { println("...lost!"); loseCounter++ }
            ) {
                sender.send(protocol.println1bpp(bitmap.row(y)))
                println("...done!")
            }
        }
        println("Lost: $loseCounter of ${bitmap.height}")
    }

    private suspend fun withLoseOnTimeout(
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