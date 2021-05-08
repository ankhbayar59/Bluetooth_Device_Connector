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

    private val bleDeviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()
    private val bleQueue = ConcurrentLinkedQueue<bleDeviceData>()
    private var onWait: bleDeviceData? = null
    private fun BluetoothDevice.isConnected() = bleDeviceGattMap.containsKey(this)
    fun servicesOnDevice(device: BluetoothDevice): List<BluetoothGattService>? =
        bleDeviceGattMap[device]?.services



    fun connect(device: BluetoothDevice, context: Context) {
        Log.i("connect", "inConnect")
        if (device.isConnected()) {
        } else {
            addQueue(Connect(device, context.applicationContext))
        }

    }

    fun closeConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            addQueue(Disconnect(device))
        }
    }

    fun readCharacteristic(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        if (device.isConnected()) {
            addQueue(readingDataFromCharacteristic(device, characteristic.uuid))
        }
    }


    fun writeCharacteristic(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic,
        writeData: ByteArray
    ) {
        val writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        if (device.isConnected()) {
            addQueue(writingDataToCharacteristic(device, characteristic.uuid, writeType, writeData))
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
        onWait = null
        if (bleQueue.isNotEmpty()) {
            start()
        }
    }

    private fun start() {
        if (onWait != null) {
            return
        }


        val nextOperation = bleQueue.poll() ?: run {
            return
        }
        onWait = nextOperation

        if (nextOperation is Connect) {
            with(nextOperation) {
                device.connectGatt(context, false, callback)
            }
            return
        }


        val gatt = bleDeviceGattMap[nextOperation.device]
            ?: this@bleDeviceObject.run {
                end()
                return
            }
        when (nextOperation) {
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bleDeviceGattMap[gatt.device] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    closeConnection(gatt.device)
                }
            } else {
                if (onWait is Connect) {
                    end()
                }
                closeConnection(gatt.device)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (onWait is Connect) {
                end()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (onWait is readingDataFromCharacteristic) {
                end()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (onWait is writingDataToCharacteristic) {
                end()
            }
        }
    }
    fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services?.forEach { service ->
            service.characteristics?.firstOrNull { characteristic ->
                characteristic.uuid == uuid
            }?.let { matchingCharacteristic ->
                return matchingCharacteristic
            }
        }
        return null
    }

}