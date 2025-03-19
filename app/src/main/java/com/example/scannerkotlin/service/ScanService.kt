package com.example.scannerkotlin.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import java.security.Provider.Service

class ScanService : android.app.Service() {

    private var scanReceiver: BroadcastReceiver? = null



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action != null && action == "com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST") {
                    val scanData = intent.getStringExtra("EXTRA_BARCODE_DECODING_DATA")
                    val symbology = intent.getStringExtra("EXTRA_BARCODE_DECODING_SYMBOLE")

                    val localIntent = Intent("Scan_data_received")
                    localIntent.putExtra("scanData", scanData)
                    localIntent.putExtra("symbology", symbology)

                    sendBroadcast(localIntent)
                }
            }

        }
        val filter = IntentFilter("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST")
        registerReceiver(scanReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scanReceiver?.let {
            unregisterReceiver(it)
        }
        scanReceiver = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}