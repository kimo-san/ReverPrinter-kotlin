package com.kimo.reverprint.androidData

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
import com.kimo.reverprint.tools.bluetooth.BleCharacteristic
import com.kimo.reverprint.tools.bluetooth.BluetoothDevice
import com.kimo.reverprint.tools.bluetooth.BluetoothLeController
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.util.UUID

typealias AndroidBluetoothDevice = android.bluetooth.BluetoothDevice

@Suppress("DEPRECATION")
class AndroidBluetoothLeController(private val context: Context) : BluetoothLeController {

    private var txCharacteristic: BleCharacteristic? = null
    private var rxCharacteristic: BleCharacteristic? = null

    override fun setReadCharacteristic(rxCharacteristic: BleCharacteristic?) {
        this.rxCharacteristic = rxCharacteristic
    }

    override fun setWriteCharacteristic(txCharacteristic: BleCharacteristic?) {
        this.txCharacteristic = txCharacteristic
    }

    override var connectedToDevice = MutableStateFlow<BluetoothDevice?>(null)

    private var deviceGatt: BluetoothGatt? = null
    private val bluetoothAdapter by lazy { context.getSystemService(BluetoothManager::class.java)?.adapter }
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var gattCallback: GattCallback? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun discovery(): Flow<BluetoothDevice> = callbackFlow {

        bluetoothReceiver = getFoundDeviceReceiver(
            onFound = { trySend(it) },
            onRelease = { close() }
        )

        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
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
    override suspend fun connect(device: BluetoothDevice) {
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
    override fun read(): Flow<ByteArray> = callbackFlow {
        val rxCharacteristic = rxCharacteristic

        val availableChars = deviceGatt?.services
            ?.flatMap { it.characteristics }
            ?.filterNotNull()
            ?.let { characteristics ->
                if (rxCharacteristic != null) characteristics
                    .filter { it.uuid == rxCharacteristic.uuid }
                else characteristics
            }

        availableChars?.forEach { char ->
            if (deviceGatt?.setCharacteristicNotification(char, true) == true) launch {

                val descriptor = char.getDescriptor(CCCD_UUID.let(UUID::fromString))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                deviceGatt?.writeDescriptor(descriptor)

                gattCallback?.awaitNotifications(char.uuid) {
                    it.getOrNull()?.let { bytes -> send(bytes) }
                }
            }
        }

        awaitClose {
            availableChars?.forEach {
                deviceGatt?.setCharacteristicNotification(it, false)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun send(data: ByteArray) =
        suspendCancellableCoroutine { continuation ->

            deviceGatt
                ?: continuation.resumeWith(Result.failure(IllegalStateException("Device gatt not found")))
                    .also {
                        return@suspendCancellableCoroutine
                    }
            gattCallback
                ?: continuation.resumeWith(Result.failure(IllegalStateException("Gatt callback is not initialized")))
                    .also {
                        return@suspendCancellableCoroutine
                    }
            txCharacteristic
                ?: continuation.resumeWith(Result.failure(IllegalStateException("UUID is not given.")))
                    .also {
                        return@suspendCancellableCoroutine
                    }

            val txCharacteristic = txCharacteristic!!
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
    private suspend fun AndroidBluetoothDevice.bluetoothGatt() =
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
                    AndroidBluetoothDevice.TRANSPORT_LE,
                    AndroidBluetoothDevice.PHY_LE_1M
                )
            else connectGatt(context, false, callback)

            continuation.invokeOnCancellation {
                gatt.close()
            }
        }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun AndroidBluetoothDevice.toDomain(): BluetoothDevice =
    BluetoothDevice(name, address)

private class GattCallback : BluetoothGattCallback() {

    fun awaitConnection(block: (Result<BluetoothGatt>) -> Unit) {
        onConnect = block
    }

    fun awaitWriteResult(block: (Result<Unit>) -> Unit) {
        onWriteResult = block
    }

    fun awaitDisconnect(block: () -> Unit) {
        onDisconnect = block
    }

    suspend fun awaitNotifications(uuid: UUID, block: suspend (Result<ByteArray>) -> Unit) {
        notifications.collect { (emitUuid, bytes) ->
            if (emitUuid == uuid)
                block(Result.success(bytes))
        }
    }

    private var onDisconnect: (() -> Unit)? = null
    private var onConnect: ((Result<BluetoothGatt>) -> Unit)? = null
    private var onWriteResult: ((Result<Unit>) -> Unit)? = null
    private var notifications = MutableSharedFlow<Pair<UUID, ByteArray>>(
        replay = REPLAY_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_LATEST
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
        notifications.tryEmit(characteristic.uuid to value)
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

@Suppress("DEPRECATION")
private fun getFoundDeviceReceiver(
    onFound: (BluetoothDevice) -> Unit,
    onRelease: () -> Unit
) = object : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AndroidBluetoothDevice.ACTION_FOUND -> {
                val device =
                    intent.getParcelableExtra<AndroidBluetoothDevice>(AndroidBluetoothDevice.EXTRA_DEVICE)
                device?.toDomain()?.let(onFound)
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                onRelease()
            }
        }
    }
}


private const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
private const val REPLAY_BUFFER = 100