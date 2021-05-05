

package com.example.firealarmsystemcontrol

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.search.*

import android.widget.Button



class SearchActivity : AppCompatActivity() {

    private var isScanning = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)
        val stopScan = findViewById<Button>(R.id.stopScan)
        stopScan.setOnClickListener {
            //boolean variable isScanning is used to check the status of the scan operation
            //and used to stop or start scan through the button click
            if(isScanning) {
                isScanning = false
                stopScanning()
            }
            else{
                isScanning = true
                scanBLE()
            }
        }
        setupRecyclerView()
        //transitioning from the connectActivity through the press of "SCAN" button
        //needs to start scan
        scanBLE()

    }

    //making sure to stop bluetooth scan if we decide to leave this activity
    //to reduce power consumption and not have the bluetooth scan running on the background
    override fun onPause() {
        super.onPause()
        isScanning = false
        stopScanning()
    }

    private fun setupRecyclerView() {
            recyclerViewForScanner.apply {
            adapter = bleScanAdapter
            layoutManager = LinearLayoutManager(
                this@SearchActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val searchResultAnimator = recyclerViewForScanner.itemAnimator
        if (searchResultAnimator is SimpleItemAnimator) {
            searchResultAnimator.supportsChangeAnimations = false
        }
    }
    private val scanResults = mutableListOf<ScanResult>()
    private val bleScanAdapter: ScannerAdapter by lazy {
        ScannerAdapter(scanResults) { result ->
            //with bluetooth low energy is recommended to stop scanning
            // when trying to establish connection to bluetooth device
            stopScanning()
            with(result.device){
                Log.w("ScannerAdapter", "Connecting to bluetooth low energy device")
                connectGatt(applicationContext, false, gattCallback)
            }


        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val devicemacaddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Connected to $devicemacaddress")
                    // TODO: Store a reference to BluetoothGatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Disconnected from $devicemacaddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $devicemacaddress! Disconnecting...")
                gatt.close()
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // filter is used to reduce amount of searching by only looking for bluetooth low energy devices
    // with specific UUID that our bluetooth low energy fire alarm device is advertising

    /*
    private val filterForScanning = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString(ENVIRONMENTAL_SERVICE_UUID.toString())
    ).build()
    */

    //scanModeSettings will be used for parameter for
    // specifying the scan mode to be balanced to keep it sho
    // to find our bluetooth low energy fire alarm device
    private val scanModeSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, scanresult: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == scanresult.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = scanresult
                bleScanAdapter.notifyItemChanged(indexQuery)
            } else {
                with(scanresult.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}; address: $address")
                }
                scanResults.add(scanresult)
                bleScanAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private fun stopScanning() {
        bluetoothScanner.stopScan(scanCallback)
    }

    private fun scanBLE() {
        // Based on the change in requirements for Android version M or up,
        // location permission must be granted in order to scan for bluetooth low energy
        // if location permission is granted already, we can start scanning for ble devices
        // before every scan, the populated list of results from previous should be cleared

        scanResults.clear()
        bleScanAdapter.notifyDataSetChanged()
        bluetoothScanner.startScan(null, scanModeSettings, scanCallback)
    }
}