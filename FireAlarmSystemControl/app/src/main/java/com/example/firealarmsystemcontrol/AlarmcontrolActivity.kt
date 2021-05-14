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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.alarmcontrol.*
import org.jetbrains.anko.alert
import java.util.*


class AlarmcontrolActivity : AppCompatActivity() {


    private lateinit var device: BluetoothDevice


    private var isNotified : Boolean = false

    // one for triggering the siren

    private var stringValueForOne : String = "1"

    // zero for silencing the siren

    private var stringValueForZero : String = "0"

    // data being sent to be written into the siren characteristic

    private var onWriteData = stringValueForOne.hexToBytes()

    private val onReadData = stringValueForOne.hexToBytes()

    private val sirenCharacteristicUUID: ParcelUuid = ParcelUuid.fromString("00010001-2345-2312-9231-40f6e305f9EE")

    // same thing for the sensor characteristic as siren characteristic

    private val sensorCharacteristicUUID = ParcelUuid.fromString("00010010-2345-1122-9231-40f6e305f96d")

    private val characteristics by lazy {
        connectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }

    private var isSilence : Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        connectionManager.registerListener(listenerFromAlarmControlActivity)
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.alarmcontrol)
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                ?: error("Missing BluetoothDevice from MainActivity!")

        val sirenCharacteristicIndex = characteristics.indexOfFirst {
            it.uuid == sirenCharacteristicUUID.uuid }

        val sensorCharacteristicIndex = characteristics.indexOfFirst {
            it.uuid == sensorCharacteristicUUID.uuid }
        alertLottie.visibility = View.GONE
        // from connection manager we getting the list of all the characteristics that are under the

        // since we wrote and have access to the firmware code on the raspberry pi
        // using specific UUID for the siren characteristic



        // actions for the specific buttons
        // siren button triggers the alarm on the chosen bluetooth low energy fire alarm system
        // we are writing 1 as bytesArray
        // hexToBytes() function transforms hex value represented as string
        // to byteArray which is standard data type for characteristic

        val sirenButton = findViewById<Button>(R.id.siren)
        sirenButton.setOnClickListener {
            onWriteData = stringValueForOne.hexToBytes()
            connectionManager.writeCharacteristic(device, characteristics[sirenCharacteristicIndex], onWriteData)
        }
        val silenceButton = findViewById<Button>(R.id.silence)
        silenceButton.setOnClickListener {
            isSilence = true;
            onWriteData = stringValueForZero.hexToBytes()
            connectionManager.writeCharacteristic(device, characteristics[sirenCharacteristicIndex], onWriteData)
        }
        val notifyButton = findViewById<Button>(R.id.notify)
        notifyButton.setOnClickListener {
            if (!isNotified){
                //once the user is notified to the alarm once, no need to enable notify again
                isNotified = true;
                connectionManager.enableNotifications(device, characteristics[sensorCharacteristicIndex])
            }
        }
    }
    override fun onDestroy() {
        connectionManager.unregisterListener(listenerFromAlarmControlActivity)
        connectionManager.closeConnection(device)
        super.onDestroy()
    }

    private val listenerFromAlarmControlActivity by lazy {
        ConnectionListener().apply {
            onWrite = { _, characteristic ->
                runOnUiThread {
                    if (isSilence) {
                        alertLottie.visibility = View.GONE
                        normalLottie.visibility = View.VISIBLE
                        normalLottie.playAnimation()
                        isSilence = false
                    }

                    else
                    {
                        normalLottie.visibility = View.GONE
                        alertLottie.visibility = View.VISIBLE
                        alertLottie.playAnimation()
                    }


                }
            }

            onChanged = { _, characteristic ->
                runOnUiThread() {
                    normalLottie.visibility = View.GONE
                    alertLottie.visibility = View.VISIBLE
                    alertLottie.playAnimation()
                }
            }
        }
    }


    private fun String.hexToBytes() =
            this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()
}