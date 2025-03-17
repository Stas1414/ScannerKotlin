package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.BarcodeAdapter
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.model.Barcode
import com.example.scannerkotlin.request.ProductOfferRequest
import com.example.scannerkotlin.service.ScanService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ScanActivity : AppCompatActivity() {

    private val products: MutableList<Barcode> = mutableListOf()
    private lateinit var adapter: BarcodeAdapter
    private var scanDataReceiver: BroadcastReceiver? = null

    @SuppressLint("MissingInflatedId", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activity)

        startService(Intent(this, ScanService::class.java))

        val recyclerView: RecyclerView = findViewById(R.id.recycleViewScan)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BarcodeAdapter(products)
        recyclerView.adapter = adapter

        val baseUrl = "https://bitrix.izocom.by/rest/1/zkfdpw7kuo0xs9t6/"

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiBitrix = retrofit.create(ApiBitrix::class.java)

        var productOfferRequest = ProductOfferRequest()
//        productOfferRequest.filter[]

        scanDataReceiver = object : BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action != null && action == "Scan_data_received") {
                    val product = Barcode(
                        intent.getStringExtra("scanData").toString(),
                        intent.getStringExtra("symbology").toString()
                    )

                    if (product.checkInList(products)) showAlertInfo() else {

                        products.add(0, product)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        val intentFilter = IntentFilter("Scan_data_received")
        registerReceiver(scanDataReceiver, intentFilter)
    }

    private fun showAlertInfo() {
        AlertDialog.Builder(this).apply {
            setTitle("Предупреждение")
            setMessage("Этот товар уже отсканирован")
            setCancelable(false)
            setPositiveButton("Закрыть") { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (scanDataReceiver != null) {
            unregisterReceiver(scanDataReceiver)
        }
    }
}