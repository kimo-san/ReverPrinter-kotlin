package com.kimo.reverprint.providers.tinyprint

import com.kimo.reverprint.domain.printer.PrintMode
import com.kimo.reverprint.domain.printer.PrinterCapabilities
import com.kimo.reverprint.domain.printer.ThermalPrinter
import com.kimo.reverprint.tools.bluetooth.BluetoothLeCharacteristic
import com.kimo.reverprint.tools.bluetooth.BluetoothController
import com.kimo.reverprint.tools.bluetooth.BluetoothDevice
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TinyprintBluetoothController(
    private val hardwareController: BluetoothLeController
) {

    val scope = CoroutineScope(Dispatchers.IO)

    val protocol get() = _protocol as DeviceProtocol
    private val _protocol = getNewFittableProtocol()

    val pairedDevice = hardwareController.connectedToDevice
        .map { bleDev ->
            val devName = bleDev?.name ?: return@map null

            DevCapabilities.supportedPrinters.entries.find { (_, props) ->
                devName.startsWith(props.headName)
            }?.let { (k, v) ->
                _protocol.fitToDevice(v)
                ThermalPrinter(k, v, bleDev)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)


    fun discovery(): Flow<ThermalPrinter> =
        hardwareController.discovery()
            .map { bleDev ->
                val (key, tinDev) = asTinyprintDevice(bleDev) ?: return@map null
                ThermalPrinter(key, tinDev, bleDev)
            }
            .filterNotNull()

    suspend fun connect(device: ThermalPrinter) {
        hardwareController.connect(device.asBluetoothDevice())
    }

    fun disconnect() {
        hardwareController.disconnect()
    }

    suspend fun stream(block: suspend BluetoothLeController.() -> Unit) =
        withContext(Dispatchers.IO) {
            val checker = BluetoothDeviceChecker(this, hardwareController, protocol)
            GracefulController(checker).block()
            checker.close()
        }


    private inner class GracefulController(
        val checker: BluetoothDeviceChecker
    ) : BluetoothLeController by hardwareController {
        override suspend fun send(data: ByteArray) {
            checker.suspendIfNeeded()
            hardwareController.send(data)
        }
    }

    private fun ThermalPrinter.asBluetoothDevice() =
        BluetoothDevice(
            name = null,
            address = macAddress
        )

    private fun ThermalPrinter(
        key: String,
        nativeCaps: DevCapabilities,
        found: BluetoothDevice
    ) = ThermalPrinter(
        name = key,
        macAddress = found.address,
        capabilities = PrinterCapabilities(
            printWidth = nativeCaps.printSize,
            supportedModes = buildList {
                if (nativeCaps.isGrayPrint) add(PrintMode.BPP4)
                add(PrintMode.BPP1)
            }
        )
    )

    private fun asTinyprintDevice(bleDev: BluetoothDevice): Map.Entry<String, DevCapabilities>? {
        bleDev.name ?: return null
        return DevCapabilities.supportedPrinters.entries.find { (_, props) ->
            bleDev.name.startsWith(props.headName)
        }
    }

    init {
        hardwareController.setReadCharacteristic(BluetoothLeCharacteristic(READ_UUID))
        hardwareController.setWriteCharacteristic(BluetoothLeCharacteristic(WRITE_UUID))
    }
}

private class BluetoothDeviceChecker(
    scope: CoroutineScope,
    bluetoothController: BluetoothController,
    protocol: DeviceProtocol
) {

    suspend fun suspendIfNeeded() {
        if (isFull.value) isFull
            .also { println("BluetoothDeviceChecker: Suspending...") }
            .filter { !it }
            .first()
            .also { println("BluetoothDeviceChecker: Device is free again!") }
    }

    private var job: Job = scope.launch {
        bluetoothController.read()
            .map { protocol.parseReceivedMessage(it) }
            .filterIsInstance<DeviceProtocol.Answer.IsOverloaded>()
            .map { it.isOverloaded }
            .collect { isFull.value = it }
    }

    fun close() {
        job.cancel()
    }

    private val isFull = MutableStateFlow(false)
}


private const val WRITE_UUID = "0000AE01-0000-1000-8000-00805F9B34FB"
private const val READ_UUID = "0000AE02-0000-1000-8000-00805F9B34FB"