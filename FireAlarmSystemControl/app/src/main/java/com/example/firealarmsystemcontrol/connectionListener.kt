package com.example.firealarmsystemcontrol
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

class ConnectionListener {
    var onConnect: ((BluetoothGatt) -> Unit)? = null
    var onDisconnect: ((BluetoothDevice) -> Unit)? = null
    var onChanged: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
    var onWrite: ((BluetoothDevice, BluetoothGattCharacteristic) -> Unit)? = null
}