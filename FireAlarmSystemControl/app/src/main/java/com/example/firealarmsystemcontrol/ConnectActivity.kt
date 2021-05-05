package com.example.firealarmsystemcontrol


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


const val LOCATION_PERMISSION_REQUEST_CODE = 1
class ConnectActivity : AppCompatActivity() {

    private val grantedLocationPermission
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect)
        enableLocation()
        val scanButton = findViewById<Button>(R.id.scan)
        scanButton.setOnClickListener {
            val intentSearch = Intent(this, SearchActivity::class.java)
            startActivity(intentSearch)
        }
    }


    private fun enableLocation () {
        // sending alert to user about why location permission must be granted
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Permission Required")
        builder.setMessage("The system requires the app to be granted location"
                + "permission in order to scan for bluetooth low energy devices")
        builder.setPositiveButton("Understood") { dialogInterface: DialogInterface, i: Int ->
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_PERMISSION_REQUEST_CODE)
        }
        builder.show()
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

}