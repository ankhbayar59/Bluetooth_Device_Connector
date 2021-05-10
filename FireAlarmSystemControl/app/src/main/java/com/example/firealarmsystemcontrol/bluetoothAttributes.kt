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

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.UUID


sealed class bluetoothAttributes {
    abstract val device: BluetoothDevice
}
data class toNotify(override val device: BluetoothDevice,
                               val characteristicUuid: UUID
) : bluetoothAttributes()

data class DescriptorWrite(
    override val device: BluetoothDevice,
    val descriptorUUID: UUID,
    val onWriteDescriptorData: ByteArray
) : bluetoothAttributes() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DescriptorWrite

        if (device != other.device) return false
        if (descriptorUUID != other.descriptorUUID) return false
        if (!onWriteDescriptorData.contentEquals(other.onWriteDescriptorData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + descriptorUUID.hashCode()
        result = 31 * result + onWriteDescriptorData.contentHashCode()
        return result
    }
}

/** Read the value of a descriptor represented by [descriptorUuid] */
data class DescriptorRead(
    override val device: BluetoothDevice,
    val descriptorUUID: UUID
) : bluetoothAttributes()

data class Connect(override val device: BluetoothDevice, val context: Context) : bluetoothAttributes()
data class Disconnect(override val device: BluetoothDevice) : bluetoothAttributes()

data class toWrite(
    override val device: BluetoothDevice,
    val characteristicUUID: UUID,
    val onWriteType: Int,
    val onWriteData: ByteArray
) : bluetoothAttributes() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as toWrite

        if (device != other.device) return false
        if (characteristicUUID != other.characteristicUUID) return false
        if (onWriteType != other.onWriteType) return false
        if (!onWriteData.contentEquals(other.onWriteData)) return false

        return true
    }
    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + characteristicUUID.hashCode()
        result = 31 * result + onWriteType
        result = 31 * result + onWriteData.contentHashCode()
        return result
    }
}
data class toRead(
    override val device: BluetoothDevice,
    val characteristicUUID: UUID
) : bluetoothAttributes()


