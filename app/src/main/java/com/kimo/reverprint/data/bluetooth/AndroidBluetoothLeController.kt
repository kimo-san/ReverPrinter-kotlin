package com.kimo.reverprint.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID

typealias IBluetoothDevice = com.kimo.reverprint.data.bluetooth.BluetoothDevice

@Suppress("DEPRECATION")
class AndroidBluetoothLeController(
    private val context: Context,
    private val txCharacteristic: BleCharacteristic?,
    private val rxCharacteristic: BleCharacteristic?
): BluetoothController {

    override var connectedToDevice = MutableStateFlow<IBluetoothDevice?>(null)

    private var deviceGatt: BluetoothGatt? = null
    private val bluetoothAdapter by lazy { context.getSystemService(BluetoothManager::class.java)?.adapter }
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var gattCallback: GattCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun discover(): Flow<IBluetoothDevice> = callbackFlow {

        bluetoothReceiver = getFoundDeviceReceiver(
            onFound = { trySend(it) },
            onRelease = { close() }
        )

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        context.registerReceiver(bluetoothReceiver, filter)
        bluetoothAdapter?.startDiscovery()

        awaitClose @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN) {
            context.unregisterReceiver(bluetoothReceiver)
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override suspend fun connect(device: IBluetoothDevice) {
        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter?.getRemoteDevice(device.address)?.bluetoothGatt()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        deviceGatt?.close()
        deviceGatt = null
        gattCallback = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun read(): Flow<ByteArray> {
        return callbackFlow {

            val chars = deviceGatt?.services?.flatMap { it.characteristics }?.filterNotNull()

            chars?.forEach { char ->
                if (deviceGatt?.setCharacteristicNotification(char, true) == true) {
                    println("Listening... ${char.uuid}")

                    launch {
                        gattCallback?.awaitNotifications(char.uuid) {
                            it.getOrNull()?.let { element ->
                                trySendBlocking(element)
                            }
                        }
                    }
                }            }

            awaitClose {
                chars?.forEach {
                    deviceGatt?.setCharacteristicNotification(it, false)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun send(data: ByteArray) =
        suspendCancellableCoroutine { continuation ->

            deviceGatt
                ?: continuation.resumeWith(Result.failure(IllegalStateException("Device gatt not found"))).also {
                    return@suspendCancellableCoroutine
                }
            gattCallback
                ?: continuation.resumeWith(Result.failure(IllegalStateException("Gatt callback is not initialized"))).also {
                    return@suspendCancellableCoroutine
                }
            txCharacteristic
                ?: continuation.resumeWith(Result.failure(IllegalStateException("UUID is not given."))).also {
                    return@suspendCancellableCoroutine
                }

            val gatt = deviceGatt!!
            val callback = gattCallback!!

            callback.awaitWriteResult {
                continuation.resumeWith(it)
            }

            val characteristic = gatt.services.flatMap { it.characteristics }
                .find { it.uuid == txCharacteristic.uuid }.let {
                    if (it == null) {
                        continuation.resumeWith(Result.failure(IllegalStateException("Characteristic not found")))
                        return@suspendCancellableCoroutine
                    } else it
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    data,
                    characteristic.writeType
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                gatt.writeCharacteristic(characteristic)
            }

        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun BluetoothDevice.bluetoothGatt() =
        suspendCancellableCoroutine { continuation ->

            val callback = GattCallback()
            callback.apply {
                awaitDisconnect {
                    connectedToDevice.update { null }
                }
                awaitConnection { gattResult ->
                    gattResult.onSuccess { gatt ->
                        deviceGatt = gatt
                        gattCallback = callback
                        connectedToDevice.update { gatt.device?.toDomain() }
                        continuation.resumeWith(Result.success(Unit))
                    }.onFailure {
                        continuation.resumeWith(Result.failure(it))
                    }
                }
            }

            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                connectGatt(
                    context, false, callback,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M
                )
            else connectGatt(context, false, callback)

            continuation.invokeOnCancellation {
                gatt.close()
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun BluetoothDevice.toDomain() : IBluetoothDevice =
        IBluetoothDevice(
            name = name,
            address = address
        )

    private class GattCallback: BluetoothGattCallback() {

        fun awaitConnection(block: (Result<BluetoothGatt>) -> Unit) {
            onConnect = block
        }
        fun awaitWriteResult(block: (Result<Unit>) -> Unit) {
            onWriteResult = block
        }
        fun awaitDisconnect(block: () -> Unit) {
            onDisconnect = block
        }
        suspend fun awaitNotifications(uuid: UUID, block: (Result<ByteArray>) -> Unit) {
            onNotification.collect {
                it[uuid]?.let {
                    block(Result.success(it))
                }
            }
        }

        private var onDisconnect: (() -> Unit)? = null
        private var onConnect: ((Result<BluetoothGatt>) -> Unit)? = null
        private var onWriteResult: ((Result<Unit>) -> Unit)? = null
        private var onNotification = MutableStateFlow(
            mutableMapOf<UUID, ByteArray>()
        )

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    onWriteResult?.invoke(Result.success(Unit))
                }
                BluetoothGatt.GATT_FAILURE -> {
                    onWriteResult?.invoke(Result.failure(IllegalStateException("Could not write to device")))
                }
            }
            onWriteResult = null
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            onNotification.update {
                it.put(characteristic.uuid, value); it
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> Unit
                BluetoothGatt.GATT_FAILURE -> {
                    onConnect?.invoke(Result.failure(IllegalStateException("Could not connect to device")))
                    onConnect = null
                }
            }

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    gatt.close()
                    onDisconnect?.invoke()
                    onDisconnect = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    onConnect?.invoke(Result.success(gatt))
                }
                BluetoothGatt.GATT_FAILURE -> {
                    onConnect?.invoke(Result.failure(IllegalStateException("Could not find services")))
                }
            }
            onConnect = null
        }
    }

    fun getFoundDeviceReceiver(
        onFound: (IBluetoothDevice) -> Unit,
        onRelease: () -> Unit
    ) = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.toDomain()?.let(onFound)
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    onRelease()
                }
            }
        }
    }
}