package com.example.firealarmsystemcontrol


import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val connectButton = findViewById<Button>(R.id.control)
        val controlButton = findViewById<Button>(R.id.connect)

        connectButton.setOnClickListener {
            val intentConnect = Intent(this, ControlActivity::class.java)
            startActivity(intentConnect)
        }

        controlButton.setOnClickListener {
            val intentControl = Intent(this, ConnectActivity::class.java)
            startActivity(intentControl)
        }
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val intentBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Enable Bluetooth")
            builder.setMessage("Bluetooth must be enabled on the smartphone to use the application")
            builder.setPositiveButton("Understood") { dialogInterface: DialogInterface, i: Int ->
                startActivityForResult(intentBluetooth, ENABLE_BLUETOOTH_REQUEST_CODE)
            }
            builder.show()
        }
    }
    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            enableBluetooth()
        }
    }
}