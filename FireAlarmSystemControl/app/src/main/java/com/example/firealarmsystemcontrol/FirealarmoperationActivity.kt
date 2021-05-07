package com.example.firealarmsystemcontrol

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.MenuItem

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.firealarmoperation.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.selector



import java.util.*


class FirealarmoperationActivity : AppCompatActivity() {

    private lateinit var device: BluetoothDevice
    var byteString : String = "1"
    var writeData = byteString.hexToBytes()

    private val characteristics by lazy {
        bleDeviceObject.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }


    private val characteristicProperties by lazy {
        characteristics.map { characteristic ->
            characteristic to mutableListOf<CharacteristicProperty>().apply {
                if (characteristic.allowsRead()) add(CharacteristicProperty.Readable)
                if (characteristic.allowsWrite()) add(CharacteristicProperty.Writable)
            }.toList()
        }.toMap()
    }
    private var notifyingCharacteristics = mutableListOf<UUID>()

    private val characteristicAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(characteristics) { characteristic ->
            showCharacteristicOptions(characteristic)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        bleDeviceObject.registerListener(searchListener)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")
        setContentView(R.layout.firealarmoperation)
        initRecyclerViewForCharacteristics()
    }

    override fun onDestroy() {
        bleDeviceObject.unregisterListener(searchListener)
        bleDeviceObject.closeConnection(device)
        super.onDestroy()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun initRecyclerViewForCharacteristics() {
        characteristic_viewer.apply {
            adapter = characteristicAdapter
            layoutManager = LinearLayoutManager(
                this@FirealarmoperationActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = characteristic_viewer.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }


    private fun showCharacteristicOptions(characteristic: BluetoothGattCharacteristic) {
        characteristicProperties[characteristic]?.let { properties ->

            // Based on characteristic property, read or write operation is done to the characteristic

            selector("Provided Actions", properties.map { it.action }) { _, i ->
                when (properties[i]) {
                    CharacteristicProperty.Readable -> {
                        bleDeviceObject.readCharacteristic(device, characteristic)
                    }
                    CharacteristicProperty.Writable -> {
                        bleDeviceObject.writeCharacteristic(device, characteristic, writeData)
                    }
                    CharacteristicProperty.Notifiable, CharacteristicProperty.Indicatable -> {
                        if (notifyingCharacteristics.contains(characteristic.uuid)) {
                            //ConnectionManager.disableNotifications(device, characteristic)
                        } else {
                            //ConnectionManager.enableNotifications(device, characteristic)
                        }
                    }
                }
            }
        }
    }

    private val searchListener by lazy {
        ConnectionListener().apply {
            onDisconnect = {
                alert {
                    title = "Disconnected!"
                    positiveButton("OK") { onBackPressed() }
                }.show()
            }
            onCharacteristicRead = { _, characteristic ->
                runOnUiThread() {
                    alert {
                        title = "Read Value"
                        message = "Value of Characteristic: ${characteristic.value.toHexString()}"
                        positiveButton("OK") {}
                    }.show()
                }
            }
        }
    }

    private enum class CharacteristicProperty {
        Readable,
        Writable,
        Notifiable,
        Indicatable;

        val action
            get() = when (this) {
                Readable -> "Read"
                Writable -> "Write"
                Notifiable -> "Toggle Notifications"
                Indicatable -> "Toggle Indications"
            }
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}