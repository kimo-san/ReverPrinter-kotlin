package com.kimo.reverprint.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

typealias IBluetoothDevice = com.kimo.reverprint.data.bluetooth.BluetoothDevice
class AndroidBluetoothLeController(
    private val context: Context,
    private val inputCharacteristic: BleCharacteristic?
): BluetoothController {

    override var connectedToDevice: IBluetoothDevice? = null

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
        connectedToDevice = deviceGatt?.device?.toDomain()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() {
        deviceGatt?.close()
        deviceGatt = null
        gattCallback = null
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
            inputCharacteristic
                ?: continuation.resumeWith(Result.failure(IllegalStateException("UUID is not given."))).also {
                    return@suspendCancellableCoroutine
                }

            val gatt = deviceGatt!!
            val callback = gattCallback!!

            val characteristic = gatt.services.flatMap { it.characteristics }
                .find { it.uuid == inputCharacteristic.uuid }.let {
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

            callback.onWriteResult = {
                if (continuation.isActive)
                    continuation.resumeWith(it)
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun BluetoothDevice.bluetoothGatt() =
        suspendCancellableCoroutine { continuation ->

            val callback = GattCallback()

            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                connectGatt(
                    context, false, callback,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M
                )
            else connectGatt(context, false, callback)

            callback.onConnect = { gattResult ->
                gattResult.onSuccess {
                    deviceGatt = it
                    gattCallback = callback
                    continuation.resumeWith(Result.success(Unit))
                }.onFailure {
                    continuation.resumeWith(Result.failure(it))
                }
            }

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

    private open class GattCallback(
        var onWriteResult: (Result<Unit>) -> Unit = { },
        var onConnect: (Result<BluetoothGatt>) -> Unit = { },
    ): BluetoothGattCallback() {

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    onWriteResult(Result.success(Unit))
                }
                BluetoothGatt.GATT_FAILURE -> {
                    onWriteResult(Result.failure(IllegalStateException("Could not write to device")))
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                }
                BluetoothGatt.GATT_FAILURE -> {
                    onConnect(Result.failure(IllegalStateException("Could not connect to device")))
                }
            }

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    gatt.close()
                    onConnect(Result.failure(IllegalStateException("Could not connect to device")))
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    onConnect(Result.success(gatt))
                }
                BluetoothGatt.GATT_FAILURE -> {
                    onConnect(Result.failure(IllegalStateException("Could not find services")))
                }
            }
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