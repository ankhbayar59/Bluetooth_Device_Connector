package com.example.firealarmsystemcontrol


import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.android.synthetic.main.search.*
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import java.util.*



class SearchActivity : AppCompatActivity() {
    private val grantedLocationPermission
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // scanState boolean variable is used to keep track of the scan operation

    private var scanState : Boolean = false;
    private val scanResults= mutableListOf<ScanResult>()
    private var isFirstScan : Boolean = true;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)
        supportActionBar?.hide()
        populateFilter()
        if(!grantedLocationPermission){
            enableLocation()
        }
        val scanButton = findViewById<Button>(R.id.scanButton)
        scanButton.setOnClickListener {
            if(isFirstScan) {
                scanButton.setText("STOP")
                isFirstScan = false;
                scanState = true;
                scanBLE()
            }
            else if(scanState && !isFirstScan){
                scanButton.setText("START")
                scanState = false
                stopScanning()
            }
            else if(!scanState && !isFirstScan){
                scanButton.setText("STOP")
                scanState = true
                scanBLE()
            }
        }
        //setting up our recycler view
        initRecyclerViewForScanner()

    }

    // in the event we leave this activity, bluetooth scan must be stopped.
    //to reduce power consumption and not have the bluetooth scan running on the background
    override fun onPause() {
        super.onPause()
        if(scanState){
            scanState = false
            stopScanning()
            scanButton.setText("START")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("onResume", "At one on resume")
        connectionManager.registerListener(listenerSearch)
    }

    private val bleScanAdapter: ScannerAdapter by lazy {
        ScannerAdapter(scanResults) { result ->
            // with bluetooth low energy is recommended to stop scanning
            // when trying to establish connection to bluetooth device
            if(scanState){
                scanState = false
                scanButton.setText("START")
                stopScanning()
            }
            alert{
                title = "Connecting"
                message = "Connection to device: $result.device.name"
                positiveButton("OK"){}
            }
            // User chooses one of the fire alarm device from the search result and decides to connect to it
            with(result.device) {
                connectionManager.connect(this, this@SearchActivity)
            }
        }
    }


    // Used to initialize the recycler view
    // Because we are using scanfilter for our bluetoothscan, the recycler view will only present
    // the fire alarm devices we implemented on Raspberry Pi 3
    private fun initRecyclerViewForScanner() {
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

    private val listenerSearch by lazy {
        ConnectionListener().apply {
            Log.i("searchListener", "inside search listener")
            onConnect = { gatt ->
                    Intent(this@SearchActivity, AlarmcontrolActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    startActivity(it)
                }
                connectionManager.unregisterListener(this)
            }
            onDisconnect = {
                alert {
                    title = "Disconnect"
                    message = "Disconnected or unable to connect to device."
                    positiveButton("OK") {}
                }.show()
            }
        }
    }




    // filter is used to reduce amount of searching by only looking for bluetooth low energy devices
    // with specific UUID that our bluetooth low energy fire alarm device is advertising

    private val uuid = ParcelUuid.fromString("1bb00766-f72c-406c-8ca4-1b6fe066f6bb")
    private val filterForScanning = ScanFilter.Builder().setServiceUuid(uuid).build()
    //scanModeSettings will be used for parameter for
    // specifying the scan mode to be balanced to keep it sho
    // to find our bluetooth low energy fire alarm device
    private val scanModeSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    private var filters = mutableListOf<ScanFilter>()
    private fun populateFilter() {
        filters.add(filterForScanning)
    }
    private fun scanBLE() {
        // before every scan, the populated list of results from previous should be cleared
        scanResults.clear()
        bleScanAdapter.notifyDataSetChanged()

        bluetoothScanner.startScan(null, scanModeSettings, scanCallBack)
    }

    private fun stopScanning() {

        // simple and straightforward, telling the app platform device's bluetooth adapter to stop scanning
        // as mentioned before, it is safer and saves energy to stop bluetooth scan if not needed.
        bluetoothScanner.stopScan(scanCallBack)
    }

    private val scanCallBack = object : ScanCallback() {
        override fun onScanResult(typeOfCallBack: Int, resultFromScan: ScanResult) {
            // scanResults.indexOfFirst returns of index of the first item that is located in the list,
            // we are using the mac address to find
            // we seeing if device we scanned already exist in the list
            // if it doesn't -1 is returned
            val isExistIndex = scanResults.indexOfFirst{
                it.device.address == resultFromScan.device.address
            }

            // we just wanna check if the isExist is anything other than -1,
            // but it should be between 0 to the size of the list

            if (isExistIndex == -1) {
                // this device doesn't exist in our scanned list
                scanResults.add(resultFromScan)
                // we are notifying our recycler view for our scanned results
                // that we have made insertion into the list
                bleScanAdapter.notifyItemInserted(scanResults.size - 1)
            } else {
                // if the device is already in the list, then we just insert it
                scanResults[isExistIndex] = resultFromScan

                // letting the Recycler know itemChanged
                // the reason we still add the device it's existing index is
                // the list needs to be up to date for better user experience
                // we can skip, but the
                bleScanAdapter.notifyItemChanged(isExistIndex)
            }
        }
    }

    private fun enableLocation () {
        // sending alert to user about why location permission must be granted
        runOnUiThread {
            alert {
                title = "Location Permission Required!"
                message = "The system requires the app to be granted location " +
                        "permission in order to scan for bluetooth low energy devices"
                positiveButton("OK") {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        1
                    )
                }
            }.show()
        }
    }
}
