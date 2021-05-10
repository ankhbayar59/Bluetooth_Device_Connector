package com.example.firealarmsystemcontrol

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import org.jetbrains.anko.alert


class MainActivity : AppCompatActivity() {
    // Used to access the user/smartphone bluetooth adapter
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener {
            val intentConnect = Intent(this, SearchActivity::class.java)
            startActivity(intentConnect)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            enableBluetooth()
        }
    }
    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val intentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // Alerting the user to enable bluetooth on their device
            // in order to scan for bluetooth low energy devices
            alert {
                title = "Bluetooth Must Be Enabled!"
                message = "In order to scan for bluetooth low energy devices, " +
                        "bluetooth must be enabled on the user's device"
                positiveButton("OK") {startActivityForResult(intentBluetooth, 1)}
            }.show()
        }
    }

}