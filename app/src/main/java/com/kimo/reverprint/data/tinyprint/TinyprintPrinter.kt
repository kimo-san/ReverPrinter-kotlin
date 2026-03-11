package com.kimo.reverprint.data.tinyprint

import android.content.Context
import android.graphics.Bitmap
import com.kimo.reverprint.data.bitmaps.Grey4bpp
import com.kimo.reverprint.data.bitmaps.Monochrome
import com.kimo.reverprint.data.bitmaps.Pixels
import com.kimo.reverprint.data.bitmaps.BitmapSettings
import com.kimo.reverprint.data.bitmaps.convertViewableImage
import com.kimo.reverprint.data.bluetooth.AndroidBluetoothLeController
import com.kimo.reverprint.data.bluetooth.BleCharacteristic
import com.kimo.reverprint.data.bluetooth.BluetoothController
import com.kimo.reverprint.data.bluetooth.BluetoothDevice
import com.kimo.reverprint.domain.PrintMode
import com.kimo.reverprint.domain.Printer
import com.kimo.reverprint.domain.ThermalPrinter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

    private var pairedToDevice = MutableStateFlow<TinyprintDevice?>(null)
    private val protocol: DeviceCommunicationProtocol = ProtocolImpl()
    private val bluetoothController: BluetoothController by lazy {
        AndroidBluetoothLeController(
            context = context,
            txCharacteristic = BleCharacteristic(
                "0000AE01-0000-1000-8000-00805F9B34FB".let(UUID::fromString)
            )
        )
    }
    override val connectedTo: Flow<ThermalPrinter?> get() = pairedToDevice
        .filterNotNull()
        .combine(bluetoothController.connectedToDevice) { pair, dev ->
            dev?.let {
                ThermalPrinter(
                    name = pair.modelNo,
                    macAddress = dev.address,
                    supportedModes = buildList {
                        if (pair.isGrayPrint) add(PrintMode.BPP4)
                        add(PrintMode.BPP1)
                    }
                )
            }
        }


    override fun findAvailable(): Flow<ThermalPrinter> = channelFlow {

        bluetoothController.discover()
            .map { dev ->

                val found = TinyprintDevice.supportedPrinters.entries.find { (_, v) ->
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
                    )
                )
            }.collect()
    }

    override suspend fun connect(device: ThermalPrinter) {
        bluetoothController.connect(device.asBluetoothDevice())
        pairedToDevice.update { TinyprintDevice.supportedPrinters[device.name] }
    }

    override suspend fun generatePreviews(imageBitmap: Bitmap, configuration: Printer.Configuration): Printer.PrintPreviews =
        withContext(Dispatchers.Default) {

            val device = pairedToDevice.value ?: error("Not connected to printer")
            val previews = mutableMapOf<PrintMode, Bitmap>()
            val printable = mutableMapOf<PrintMode, Pixels>()

            launch {
                PrintMode.BPP1.apply {
                    convertViewableImage(
                        imageBitmap,
                        BitmapSettings(
                            width = device.printSize,
                            colorModel = Monochrome,
                            dither = configuration.ditherImage
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
                            width = device.printSize,
                            colorModel = Grey4bpp,
                            dither = configuration.ditherImage
                        )
                    ).also { (view, print) ->
                        previews[this] = view
                        saveBitmapDebug(context, view, "Preview-4bpp.png")
                        printable[this] = print
                    }
                }
            }
            joinAll()
            TinyprintPreview(previews, printable, configuration)
        }

    override suspend fun print(
        imagePreview: Printer.PrintPreviews,
        mode: PrintMode
    ): Unit = withContext(
        Dispatchers.Default + CoroutineExceptionHandler { c, e -> println(e.message) }
    ) {
        require(pairedToDevice.value != null)
        require(imagePreview is TinyprintPreview)
        val device = pairedToDevice.value!!
        val bitmap = imagePreview.pixelArrays[mode]

        val readJob = bluetoothController.read().onEach {
            println("FROM PRINTER: " + it.toHexString())
        }.launchIn(this)

        when (mode) {

            PrintMode.BPP1 -> {
                require(bitmap?.model is Monochrome)
                bluetoothController.send(
                    protocol.setMode(DeviceCommunicationProtocol.Mode.MONO_IMG)
                )
                bluetoothController.send(
                    protocol.setQuality(DeviceCommunicationProtocol.Quality.Five)
                )

                repeat(bitmap.height) { y ->
                    bluetoothController.send(
                        protocol.println1bpp(bitmap.getRow(y))
                    )
                }
            }

            PrintMode.BPP4 -> {
                require(bitmap?.model is Grey4bpp)
                bluetoothController.send(
                    protocol.setMode(DeviceCommunicationProtocol.Mode.GREY_IMG)
                )
                bluetoothController.send(
                    protocol.setQuality(DeviceCommunicationProtocol.Quality.Five)
                )

                repeat(bitmap.height) { y ->
                    bluetoothController.send(
                        protocol.println4bpp(bitmap.getRow(y), device.usesCompressionForGreyScale)
                    )
                    if (y % device.blockCountForGreyPrint == 0) {
                        delay(device.grayImageSpeed.toLong())
                    }
                }
            }
        }

        if (imagePreview.appliedConfiguration.addSpaceAfterPrint)
            bluetoothController.send(protocol.feedPaper(120))

        //readJob.cancelAndJoin()
    }

    override suspend fun disconnect() {
        bluetoothController.disconnect()
        pairedToDevice.update { null }
    }

    private fun ThermalPrinter.asBluetoothDevice() = BluetoothDevice(
        name = null,
        address = macAddress
    )

    private class TinyprintPreview(
        availableModes: Map<PrintMode, Bitmap>,
        val pixelArrays: Map<PrintMode, Pixels>,
        configuration: Printer.Configuration,
    ) : Printer.PrintPreviews(availableModes, configuration)
}