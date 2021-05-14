/*
*******       Copyright [2021] [SJSU Senior Design project group]

*******       Licensed under the Apache License, Version 2.0 (the "License");
*******       you may not use this file except in compliance with the License.
*******       You may obtain a copy of the License at

*******       http://www.apache.org/licenses/LICENSE-2.0

*******       Unless required by applicable law or agreed to in writing, software
*******       distributed under the License is distributed on an "AS IS" BASIS,
*******       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*******       See the License for the specific language governing permissions and
*******       limitations under the License.
*/

package com.example.firealarmsystemcontrol

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


object connectionManager {


    const val CCCD = "00002902-0000-1000-8000-00805F9B34FB"

    private var listenerSet: MutableSet<WeakReference<ConnectionListener>> = mutableSetOf()
    private val bluetoothDeviceGattMap = ConcurrentHashMap<BluetoothDevice, BluetoothGatt>()

    // bluetooth operation queue is to guard and possible prevent error from happening.
    // after start of each opereation we should wait for callback before proceeding to new one

    private val bluetoothOperationQueue = ConcurrentLinkedQueue<bluetoothAttributes>()

    // used for the bluetooth operation quueue control
    private var onWait: bluetoothAttributes? = null

    private fun BluetoothDevice.isConnected() = bluetoothDeviceGattMap.containsKey(this)
    fun servicesOnDevice(device: BluetoothDevice): List<BluetoothGattService>? =
        bluetoothDeviceGattMap[device]?.services


    // registerListener is used for controlling the listeners
    fun registerListener(listener: ConnectionListener) {
        if (listenerSet.map { it.get() }.contains(listener)) {
            return
        }
        listenerSet.add(WeakReference(listener))
        listenerSet = listenerSet.filter { it.get() != null }.toMutableSet()
    }

    fun unregisterListener(listener: ConnectionListener) {
        var unregister: WeakReference<ConnectionListener>? = null
        listenerSet.forEach {
            if (it.get() == listener) {
                unregister = it
            }
        }
        unregister?.let {
            listenerSet.remove(it)
        }
    }
    fun writeCharacteristic(
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic,
            onWriteData: ByteArray
    ) {
        // we are not checking if the characteristic we are writing to has "WRITE" Property enabled,
        // since we as programmer on the app and firmware guaranteed that it has WRITE property
        if (device.isConnected()) {
            addQueue(toWrite(device, characteristic.uuid, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, onWriteData))
        }
    }
    fun enableNotifications(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {

        // it is required that the property of the characteristic must be checked to know if it can notify or indicate
        if (device.isConnected())
        {
            addQueue(toNotify(device, characteristic.uuid))
        }
    }

    fun connect(device: BluetoothDevice, context: Context) {
        if (!device.isConnected()) {
            addQueue(Connect(device, context.applicationContext))
        }
    }

    fun closeConnection(device: BluetoothDevice) {
        if (device.isConnected()) {
            addQueue(Disconnect(device))
        }
    }

    // end of operation, opeation in process is null


    // used to insert new requested bluetooth operation are inserted into the queue
    private fun addQueue(operation: bluetoothAttributes) {
        bluetoothOperationQueue.add(operation)
        if (onWait == null) {
            end()
        }
    }

    private fun end() {
        Log.i("communicationManager", "End of operation $onWait")
        onWait = null
        // if the ongoing operation has performed and done, we move onto new one
        // if there is any operation pending in the queue, if not do nothing
        if (bluetoothOperationQueue.isNotEmpty()) {
            start()
        }
    }


    // begin the process of new operation, if no waiting operation, then just return

    private fun start() {

         // checking if there is pending operation, based on its data type or operation type,
        // it is performed
        if (onWait != null) {
            Log.i("communicationManager", "next operation is loaded.")
            return
        }



        val nextOperation = bluetoothOperationQueue.poll() ?: run {
            Log.i("communicationManager", "Empty Queue")
            return
        }
        onWait = nextOperation

        // operation waiting to be performed is pulled from top of the operation queue

        // connect to Gatt operation
        if (nextOperation is Connect) {
            with(nextOperation) {
                Log.i("communicationManager", "Connecting to ${device.address}")
                device.connectGatt(context, false, callback)
            }
            return
        }



        val gatt = bluetoothDeviceGattMap[nextOperation.device]
            ?: this@connectionManager.run {
                end()
                return
            }
        when (nextOperation) {
            is Disconnect -> with(nextOperation) {
                gatt.close()
                bluetoothDeviceGattMap.remove(device)
                listenerSet.forEach { it.get()?.onDisconnect?.invoke(device) }
                end()
            }
            is toWrite -> with(nextOperation) {
                gatt.findCharacteristic(characteristicUUID)?.let { characteristic ->
                    characteristic.writeType = onWriteType
                    characteristic.value = onWriteData
                    gatt.writeCharacteristic(characteristic)
                } ?: this@connectionManager.run {
                    end()
                }
            }
            is toNotify -> with(nextOperation) {
                gatt.findCharacteristic(characteristicUuid)?.let { characteristic ->
                    val cccdUuid = UUID.fromString(CCCD)
                    val payload = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
                        gatt.setCharacteristicNotification(characteristic, true)
                        cccDescriptor.value = payload
                        gatt.writeDescriptor(cccDescriptor)
                    }
                }
            }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(
                        "BluetoothGattCallback",
                        "onConnectionStateChange: connected to $deviceAddress"
                    )
                    bluetoothDeviceGattMap[gatt.device] = gatt
                    Handler(Looper.getMainLooper()).post {
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(
                        "BluetoothGattCallback",
                        "onConnectionStateChange: disconnected from $deviceAddress"
                    )
                    closeConnection(gatt.device)
                }
            } else {
                Log.i(
                    "BluetoothGattCallback",
                    "onConnectionStateChange: status $status encountered for $deviceAddress!"
                )
                if (onWait is Connect) {
                    end()
                }
                closeConnection(gatt.device)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(
                        "onServiceDiscovered",
                        "Discovered ${services.size} services for ${device.address}."
                    )

                    // the searchActicity listener will be told to connect to the device
                    // which is sending the device we selected to another activity where we can control the device

                    listenerSet.forEach { it.get()?.onConnect?.invoke(this) }
                } else {
                    Log.i("onServiceDiscovered", "failed to discover services based on: $status")
                    closeConnection(gatt.device)
                }
            }
            if (onWait is Connect) {
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
                        Log.i(
                            "onCharacteristicWrite",
                            "Wrote to characteristic $uuid | value: ${value}"
                        )
                        listenerSet.forEach { it.get()?.onWrite?.invoke(gatt.device, this) }
                    }

                    // we are not considering GATT_WRITE_NOT PERMITTED status, as again mentioned earlier,
                    // the app and firmware for the project is written by us, so we know "READ" property is enabled
                    else -> {
                        Log.i(
                            "onCharacteristicWrite",
                            "Error for write operation on $uuid, error: $status"
                        )
                    }
                }
            }

            // if write was our current operation in the queue, we are ending it by clearing onWait
            // so next operation can be loaded into onWait

            if (onWait is toWrite) {
                end()
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {

                Log.i(
                    "connectionManager",
                    "Characteristic that is changing $uuid | value it changed to as hex value : ${value.toHexString()}"
                )
                // from AlarmcontrolActivity, we letting the listener know that there was change in value in the characteristic
                // inside AlarmcontrolActivity, we as user handles it in the listener named "listenerFromAlarmControlAcivity"
                // not just for changed callback, we handle it for the ones we care in our project, those onWrite, onChanged
                listenerSet.forEach { it.get()?.onChanged?.invoke(gatt.device, this) }
            }
        }

        override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
        ) {

            if (descriptor.isCccd() &&
                    (onWait is toNotify)
            ) {

                end()
            } else if (!descriptor.isCccd()) {
                end()
            }
        }
    }

    // functions and values that are used for handling some of the callbacks


    private fun BluetoothGatt.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        services?.forEach { service ->
            service.characteristics?.firstOrNull { characteristic ->
                characteristic.uuid == uuid
            }?.let { matchingCharacteristic ->
                return matchingCharacteristic
            }
        }
        return null
    }

    fun BluetoothGattCharacteristic.allowsIndication(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
    fun BluetoothGattCharacteristic.allowsNotification(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0


    fun BluetoothGattDescriptor.isCccd() =
            uuid.toString().toUpperCase(Locale.US) == CCCD.toUpperCase(Locale.US)

    fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }
}