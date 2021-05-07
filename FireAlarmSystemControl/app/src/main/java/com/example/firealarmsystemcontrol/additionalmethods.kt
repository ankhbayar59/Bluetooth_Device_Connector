package com.example.firealarmsystemcontrol

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import java.util.Locale
import java.util.UUID

/** UUID of the Client Characteristic Configuration Descriptor (0x2902). */
const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"

// BluetoothGatt



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

fun BluetoothGattCharacteristic.allowsRead(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
fun BluetoothGattCharacteristic.allowsWrite(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
private fun BluetoothGattCharacteristic.allowsIndication(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
private fun BluetoothGattCharacteristic.allowsNotification(): Boolean = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
// BluetoothGattCharacteristic

fun BluetoothGattCharacteristic.printProperties(): String = mutableListOf<String>().apply {
    if (allowsRead()) add("READABLE")
    if (allowsWrite()) add("WRITABLE")
    if (isEmpty()) add("EMPTY")
}.joinToString()


fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }