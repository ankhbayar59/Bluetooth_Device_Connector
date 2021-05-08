

package com.example.firealarmsystemcontrol

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.search.*
import org.jetbrains.anko.alert
import java.util.*


class SearchActivity : AppCompatActivity() {
    private val grantedLocationPermission
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bluetoothScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    var byteString : String = "1"
    var byteStringZero : String = "0"
    var writeData = byteString.hexToBytes()

    // Used to stop or start bluetooth scan
    private val scanResults = mutableListOf<ScanResult>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search)
        supportActionBar?.hide()
        populateFilter()
        if (!grantedLocationPermission) {
            enableLocation()
        }
        val stopScan = findViewById<Button>(R.id.stopScan)
        stopScan.setOnClickListener {
            scanBLE()
            Handler(Looper.getMainLooper()).postDelayed({
                stopScanning()
                scanResults.forEach() { result ->
                    bleDeviceObject.connect(result.device, this@SearchActivity)
                }
            }, 4000)
        }
        val allsilenceButton = findViewById<Button>(R.id.allsilence)
        allsilenceButton.setOnClickListener {
            val indexOfFirstAlarm = scanResults.indexOfFirst {
                it.device.name == "fireAlarm1"
            }
            /*
            val indexOfSecondAlarm = scanResults.indexOfFirst {
                it.device.name == "fireAlarm2"
            }
            */

            val characteristicsOfFirstAlarm by lazy {
                bleDeviceObject.servicesOnDevice(scanResults.get(indexOfFirstAlarm).device)?.flatMap { service ->
                    service.characteristics ?: listOf()
                } ?: listOf()
            }
            /*
            val characteristicsOfSecondAlarm by lazy {
                bleDeviceObject.servicesOnDevice(scanResults.get(indexOfSecondAlarm).device)?.flatMap { service ->
                    service.characteristics ?: listOf()
                } ?: listOf()
            }
             */
            val firstSirenCharacteristicUUID = ParcelUuid.fromString("00010001-2345-2312-9231-40f6e305f9ee")
            val firstSirenCharacteristicIndex = characteristicsOfFirstAlarm.indexOfFirst {
                it.uuid == firstSirenCharacteristicUUID.uuid
            }
            /*
            val secondSirenCharacteristicUUID = ParcelUuid.fromString("00010001-2345-2312-9231-40f6e305f9ff")
            val secondSirenCharacteristicIndex = characteristicsOfSecondAlarm.indexOfFirst {
                it.uuid == secondSirenCharacteristicUUID.uuid
            }

             */
            writeData = byteStringZero.hexToBytes()
            bleDeviceObject.writeCharacteristic(scanResults[indexOfFirstAlarm].device,
                    characteristicsOfFirstAlarm[firstSirenCharacteristicIndex], writeData)
            /*
            bleDeviceObject.writeCharacteristic(scanResults[indexOfSecondAlarm].device,
                    characteristicsOfFirstAlarm[secondSirenCharacteristicIndex], writeData)

             */
        }
        val alltriggerButton = findViewById<Button>(R.id.alltrigger)
        alltriggerButton.setOnClickListener {
            val indexOfFirstAlarm = scanResults.indexOfFirst {
                it.device.name == "fireAlarm1"
            }
            Log.i("indexing", "past indexing and value is $indexOfFirstAlarm")
            /*
            val indexOfSecondAlarm = scanResults.indexOfFirst {
                it.device.name == "fireAlarm2"
            }

             */
            val characteristicsOfFirstAlarm by lazy {
                bleDeviceObject.servicesOnDevice(scanResults.get(indexOfFirstAlarm).device)?.flatMap { service ->
                    service.characteristics ?: listOf()
                } ?: listOf()
            }
            Log.i("characteristic", "past characteristics finding")
            /*
            val characteristicsOfSecondAlarm by lazy {
                bleDeviceObject.servicesOnDevice(scanResults.get(indexOfSecondAlarm).device)?.flatMap { service ->
                    service.characteristics ?: listOf()
                } ?: listOf()
            }

             */
            val firstSirenCharacteristicUUID = ParcelUuid.fromString("00010001-2345-2312-9231-40f6e305f9ee")
            val firstSirenCharacteristicIndex = characteristicsOfFirstAlarm.indexOfFirst {
                it.uuid == firstSirenCharacteristicUUID.uuid
            }
            Log.i("UUID", "Past UUID of characteristic")
            /*
            val secondSirenCharacteristicUUID = ParcelUuid.fromString("00010001-2345-2312-9231-40f6e305f9ff")
            val secondSirenCharacteristicIndex = characteristicsOfSecondAlarm.indexOfFirst {
                it.uuid == secondSirenCharacteristicUUID.uuid
            }

             */

            writeData = byteString.hexToBytes()
            bleDeviceObject.writeCharacteristic(scanResults[indexOfFirstAlarm].device,
                    characteristicsOfFirstAlarm[firstSirenCharacteristicIndex], writeData)
            Log.i("Write Trigger", "Past Write")
            /*
            bleDeviceObject.writeCharacteristic(scanResults[indexOfSecondAlarm].device,
                    characteristicsOfFirstAlarm[secondSirenCharacteristicIndex], writeData)

             */
        }
    }


    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            enableBluetooth()
        }
    }


    private val firstAlarm = ParcelUuid.fromString("00010000-89bd-43c8-9231-40f6e305f9bb")
    private val secondAlarm = ParcelUuid.fromString("1bb00766-f72c-406c-8ca4-1b6fe066f6cc")
    private val firstFilter = ScanFilter.Builder().setServiceUuid(firstAlarm).build()
    private val secondFilter = ScanFilter.Builder().setServiceUuid(secondAlarm).build()
    //scanModeSettings will be used for parameter for
    // specifying the scan mode to be balanced to keep it sho
    // to find our bluetooth low energy fire alarm device
    private val scanModeSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()
    private var filters = mutableListOf<ScanFilter>()
    private fun populateFilter() {
        filters.add(firstFilter)
        filters.add(secondFilter)
    }
    private fun scanBLE() {
        Log.i("scanBLE", "before bluetooth scan begins")
        bluetoothScanner.startScan(filters, scanModeSettings, scanCallBack)
    }




    private fun stopScanning() {
        // simple and straightforward, telling the app platform device's bluetooth adapter to stop scanning
        // as mentioned before, it is safer and saves energy to stop bluetooth scan if not needed.
        bluetoothScanner.stopScan(scanCallBack)
    }
    private val scanCallBack = object : ScanCallback() {
        override fun onScanResult(typeOfCallBack: Int, resultFromScan: ScanResult) {
            scanResults.add(resultFromScan)
        }
    }

    private fun enableLocation () {
        // sending alert to user about why location permission must be granted
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

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    private fun String.hexToBytes() =
            this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}


