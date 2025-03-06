package com.example.scannerkotlin

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scannerkotlin.adapter.ProductAdapter
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.service.ScanService

class MainActivity : AppCompatActivity() {

    private val products: MutableList<Product> = mutableListOf()

    private var scanDataReceiver: BroadcastReceiver? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, ScanService::class.java))

        val listProducts: ListView = findViewById(R.id.listProducts)

        val productAdapter = ProductAdapter(this, products)
        listProducts.adapter = productAdapter

        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action != null && action == "Scan_data_received") {
                    val product = Product(
                        intent.getStringExtra("scanData").toString(),
                        intent.getStringExtra("symbology").toString()
                    )

                    if (product.checkInList(products)) showAlertInfo() else {
                        products.add(0, product)
                        productAdapter.notifyDataSetChanged()
                    }
                }
            }

        }

        val intentFilter = IntentFilter("Scan_data_received")
        registerReceiver(scanDataReceiver, intentFilter)
    }

    private fun showAlertInfo() {
        val builder = AlertDialog.Builder(this).apply {
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
        if (scanDataReceiver == null) unregisterReceiver(scanDataReceiver)
    }
}