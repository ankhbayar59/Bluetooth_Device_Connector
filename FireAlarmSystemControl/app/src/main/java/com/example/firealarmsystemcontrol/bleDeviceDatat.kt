package com.example.firealarmsystemcontrol

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.UUID


sealed class bleDeviceData {
    abstract val device: BluetoothDevice
}
data class makingNotification( override val device: BluetoothDevice,
                                   val characteristicUuid: UUID
) : bleDeviceData()
data class Connect(override val device: BluetoothDevice, val context: Context) : bleDeviceData()
data class Disconnect(override val device: BluetoothDevice) : bleDeviceData()

data class writingDataToCharacteristic(
    override val device: BluetoothDevice,
    val UuidOfCharacteristic: UUID,
    val writeType: Int,
    val writeData: ByteArray
) : bleDeviceData() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as writingDataToCharacteristic

        if (device != other.device) return false
        if (UuidOfCharacteristic != other.UuidOfCharacteristic) return false
        if (writeType != other.writeType) return false
        if (!writeData.contentEquals(other.writeData)) return false

        return true
    }
    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + UuidOfCharacteristic.hashCode()
        result = 31 * result + writeType
        result = 31 * result + writeData.contentHashCode()
        return result
    }
}
data class readingDataFromCharacteristic(
    override val device: BluetoothDevice,
    val characteristicUuid: UUID
) : bleDeviceData()


