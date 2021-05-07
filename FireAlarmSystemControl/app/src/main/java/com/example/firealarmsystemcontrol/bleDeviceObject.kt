package com.example.firealarmsystemcontrol

import android.content.Context

import android.os.Handler
import android.os.Looper
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.util.Log
import java.lang.ref.WeakReference
import java.util.UUID

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


object bleDeviceObject {

    private var bleListener : MutableSet<WeakReference<ConnectionListener>> = mutableSetOf()
    private val bleDeviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private val bleQueue = ConcurrentLinkedQueue<bleDeviceData>()
    private var onWait: bleDeviceData? = null
    private fun BluetoothDevice.isConnected() = bleDeviceGattMap.containsKey(this)
    fun servicesOnDevice(device: BluetoothDevice): List<BluetoothGattService>? =
        bleDeviceGattMap[device]?.services

    fun registerListener(listener: ConnectionListener) {
        if (bleListener.map { it.get() }.contains(listener)) { return }
        bleListener.add(WeakReference(listener))
        bleListener = bleListener.filter { it.get() != null }.toMutableSet()
    }

    fun unregisterListener(listener: ConnectionListener) {
        var unregister: WeakReference<ConnectionListener>? = null
        bleListener.forEach {
            if (it.get() == listener) {
                unregister = it
            }
        }
        unregister?.let {
            bleListener.remove(it)
        }
    }

    fun connect(device: BluetoothDevice, context: Context) {
        if (device.isConnected()) {
        } else {
            addQueue(Connect(device, context.applicationContext))
        }

    }

    fun closeConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            addQueue(Disconnect(device))
        } else {
        }
    }

    fun readCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected() && characteristic.allowsRead()) {
            addQueue(readingDataFromCharacteristic(device, characteristic.uuid))
        } else if (!(characteristic.allowsRead())) {
            Log.i("readCharacteristic", "character uuid ${characteristic.uuid} that isn't readable!")
        } else if (!device.isConnected()) {
            Log.i("readCharacteristic","Not connected to ${device.address}, cannot perform characteristic read")
        }
    }


    fun writeCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        writeData: ByteArray
    ) {
        val writeType = when {
            characteristic.allowsWrite() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            else -> {
                Log.i("writeCharacteristic","Characteristic ${characteristic.uuid} cannot be written to")
                return
            }
        }
        if (device.isConnected()) {
            addQueue(writingDataToCharacteristic(device, characteristic.uuid, writeType, writeData))
        } else {
            Log.i("writeCharacteristic","Not connected to ${device.address}, cannot perform characteristic write")
        }
    }

    // Operation Queue methods

    private fun addQueue(operation: bleDeviceData) {
        bleQueue.add(operation)
        if (onWait == null) {
            end()
        }
    }


    private fun end() {
        Log.i("signEndOfOperation","End of $onWait")
        onWait = null
        if (bleQueue.isNotEmpty()) {
            start()
        }
    }

    private fun start() {
        if (onWait != null) {
            Log.i("doNextOepration","doNextOperation() called when an operation is pending! Aborting.")
            return
        }


        val nextOperation = bleQueue.poll() ?: run {
            Log.i("doNextOperation","Operation queue empty, returning")
            return
        }
        onWait = nextOperation

        if (nextOperation is Connect) {
            with(nextOperation) {
                Log.i("doNextOperation","Connecting to ${device.address}")
                device.connectGatt(context, false, callback)
            }
            return
        }


        val gatt = bleDeviceGattMap[nextOperation.device]
            ?: this@bleDeviceObject.run {
                Log.i("doNextOperation","Not connected to ${nextOperation.device.address}! Aborting $nextOperation operation.")
                end()
                return
            }
        when (nextOperation) {
            is Disconnect -> with(nextOperation) {
                //Timber.w("Disconnecting from ${device.address}")
                gatt.close()
                bleDeviceGattMap.remove(device)
                bleListener.forEach { it.get()?.onDisconnect?.invoke(device) }
                end()
            }
            is writingDataToCharacteristic -> with(nextOperation) {
                gatt.findCharacteristic(UuidOfCharacteristic)?.let { characteristic ->
                    characteristic.writeType = writeType
                    characteristic.value = writeData
                    gatt.writeCharacteristic(characteristic)
                } ?: this@bleDeviceObject.run {
                    end()
                }
            }
            is readingDataFromCharacteristic -> with(nextOperation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    gatt.readCharacteristic(characteristic)
                } ?: this@bleDeviceObject.run {
                    end()
                }
            }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BluetoothGattCallback","onConnectionStateChange: connected to $deviceAddress")
                    bleDeviceGattMap[gatt.device] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BluetoothGattCallback","onConnectionStateChange: disconnected from $deviceAddress")
                    closeConnection(gatt.device)
                }
            } else {
                Log.i("BluetoothGattCallback","onConnectionStateChange: status $status encountered for $deviceAddress!")
                if (onWait is Connect) {
                    end()
                }
                closeConnection(gatt.device)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i("onServiceDiscovered","Discovered ${services.size} services for ${device.address}.")
                    bleListener.forEach { it.get()?.onConnection?.invoke(this) }
                } else {
                    Log.i("onServiceDiscovered","Service discovery failed due to status $status")
                    closeConnection(gatt.device)
                }
            }

            if (onWait is Connect) {
                end()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("onCharacteristicRead","Read characteristic $uuid | value: ${value}")
                        bleListener.forEach { it.get()?.onCharacteristicRead?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.i("onCharacteristicRead","Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.i("onCharacteristicRead","Characteristic read failed for $uuid, error: $status")
                    }
                }
            }

            if (onWait is readingDataFromCharacteristic) {
                end()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("onCharacteristicWrite","Wrote to characteristic $uuid | value: ${value}")
                        bleListener.forEach { it.get()?.onCharacteristicWrite?.invoke(gatt.device, this) }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.i("onCharacteristicWrite","Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.i("onCharacteristicWrite","Characteristic write failed for $uuid, error: $status")
                    }
                }
            }

            if (onWait is writingDataToCharacteristic) {
                end()
            }
        }
    }
}