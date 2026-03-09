package com.kimo.reverprint.data.tinyprint

import android.content.Context
import android.graphics.Bitmap
import com.kimo.reverprint.data.bitmaps.Grey4bpp
import com.kimo.reverprint.data.bluetooth.AndroidBluetoothLeController
import com.kimo.reverprint.data.bluetooth.BleCharacteristic
import com.kimo.reverprint.data.bluetooth.BluetoothController
import com.kimo.reverprint.data.bluetooth.BluetoothDevice
import com.kimo.reverprint.data.bitmaps.Pixels
import com.kimo.reverprint.data.bitmaps.BitmapSettings
import com.kimo.reverprint.data.bitmaps.Monochrome
import com.kimo.reverprint.data.bitmaps.convertViewableImage
import com.kimo.reverprint.data.tinyprint.TinyprintPrinterBean.Companion.supportedPrinters
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.domain.ThermalPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private fun saveBitmapDebug(context: Context, bitmap: Bitmap, filename: String) {
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

class TinyprintPrinter(
    private val context: Context
) : Printer {

    override val connectedTo: MutableStateFlow<ThermalPrinter?> = MutableStateFlow(null)
    private var pairedToPrinter: TinyprintPrinterBean? = null
    private val protocol: TinyprintDeviceCommunicationProtocol = TinyprintDeviceCommunicationProtocolImpl()
    private val bluetoothController: BluetoothController by lazy {
        AndroidBluetoothLeController(
            context = context,
            inputCharacteristic = BleCharacteristic(
                "0000AE01-0000-1000-8000-00805F9B34FB".let(UUID::fromString)
            )
        )
    }

    override fun findAvailable(): Flow<ThermalPrinter> = channelFlow {

        bluetoothController.discover()
            .map { dev ->

                val found = supportedPrinters.entries.find { (_, v) ->
                    dev.name?.startsWith(v.headName) == true
                } ?: return@map null

                Triple(found.key, found.value, dev)
            }
            .filterNotNull()
            .onEach { (modelName, tpModel, bleDevice) ->

                send(
                    ThermalPrinter(
                    name = modelName,
                    macAddress = bleDevice.address,
                    supportedModes = buildList {
                        if (tpModel.isGrayPrint) add(PrintMode.BPP4)
                        add(PrintMode.BPP1)
                    }
                ))
            }.collect()
    }

    override suspend fun connect(device: ThermalPrinter) {
        bluetoothController.connect(device.asBluetoothDevice())
        connectedTo.update { device }
        pairedToPrinter = supportedPrinters[device.name]
    }

    override suspend fun generatePreviews(imageBitmap: Bitmap): Printer.PrintPreviews =
        withContext(Dispatchers.Default) {

            val printer = pairedToPrinter ?: error("Not connected to printer")
            val previews = mutableMapOf<PrintMode, Bitmap>()
            val printable = mutableMapOf<PrintMode, Pixels>()

            launch {
                PrintMode.BPP1.apply {
                    convertViewableImage(
                        imageBitmap,
                        BitmapSettings(
                            width = printer.printSize,
                            colorModel = Monochrome,
                            dither = false
                        )
                    ).also { (view, print) ->
                        previews[this] = view
                        saveBitmapDebug(context, view, "Preview-1bpp.png")
                        printable[this] = print
                    }
                }
            }

            launch {
                PrintMode.BPP4.apply {
                    convertViewableImage(
                        imageBitmap,
                        BitmapSettings(
                            width = printer.printSize,
                            colorModel = Grey4bpp,
                            dither = true
                        )
                    ).also { (view, print) ->
                        previews[this] = view
                        saveBitmapDebug(context, view, "Preview-4bpp.png")
                        printable[this] = print
                    }
                }
            }
            joinAll()
            TinyprintPreview(previews, printable)
        }

    override suspend fun print(
        imagePreview: Printer.PrintPreviews,
        mode: PrintMode
    ) {
        require(imagePreview is TinyprintPreview)
        val bitmap = imagePreview.pixelArrays[mode]

        when (mode) {

            PrintMode.BPP1 -> {
                require(bitmap?.model is Monochrome)
                bluetoothController.send(
                    protocol.setMode(TinyprintDeviceCommunicationProtocol.Mode.MONO_IMG)
                )
                bluetoothController.send(
                    protocol.setQuality(TinyprintDeviceCommunicationProtocol.Quality.Five)
                )

                repeat(bitmap.height) { y ->
                    bluetoothController.send(
                        protocol.println1bpp(bitmap.getRow(y))
                    )
                    if (y % 20 == 0)
                        delay(pairedToPrinter!!.imgPrintSpeed.toLong())
                }
            }

            PrintMode.BPP4 -> {
                require(bitmap?.model is Grey4bpp)
                bluetoothController.send(
                    protocol.setMode(TinyprintDeviceCommunicationProtocol.Mode.GREY_IMG)
                )
                bluetoothController.send(
                    protocol.setQuality(TinyprintDeviceCommunicationProtocol.Quality.Five)
                )

                repeat(bitmap.height) { y ->
                    bluetoothController.send(
                        protocol.println4bpp(bitmap.getRow(y))
                    )
                    if (y % 20 == 0)
                        delay(pairedToPrinter!!.grayImageSpeed.toLong())
                }
            }
        }

        bluetoothController.send(protocol.feedPaper(100))
    }

    override suspend fun disconnect() {
        bluetoothController.disconnect()
        connectedTo.update { null }
        pairedToPrinter = null
    }

    private fun ThermalPrinter.asBluetoothDevice() = BluetoothDevice(
        name = null,
        address = macAddress
    )

    private class TinyprintPreview(
        availableModes: Map<PrintMode, Bitmap>,
        val pixelArrays: Map<PrintMode, Pixels>,
    ) : Printer.PrintPreviews(availableModes)
}