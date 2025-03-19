package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductScanAdapter
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.mappers.ProductMeasureMapper
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.request.MeasureIdRequest
import com.example.scannerkotlin.request.ProductIdRequest
import com.example.scannerkotlin.response.MeasureResponse
import com.example.scannerkotlin.response.ProductBarcodeResponse
import com.example.scannerkotlin.response.ProductResponse
import com.example.scannerkotlin.service.ScanService
import com.google.gson.internal.LinkedTreeMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ScanActivity : AppCompatActivity() {

    private val products: MutableList<Product> = mutableListOf()
    private lateinit var adapter: ProductScanAdapter
    private var scanDataReceiver: BroadcastReceiver? = null
    private val baseUrl = "https://bitrix.izocom.by/rest/1/c953o6imkob2gpwd/"
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiBitrix: ApiBitrix = retrofit.create(ApiBitrix::class.java)

    @SuppressLint("MissingInflatedId", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activity)



        val recyclerView: RecyclerView = findViewById(R.id.recycleViewScan)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductScanAdapter(products)
        recyclerView.adapter = adapter


        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == "Scan_data_received") {
                    val barcodeData = intent.getStringExtra("scanData").toString()
                    Log.d("ScanActivity", "Scanned barcode: $barcodeData")


                    getProductIdByBarcode(barcodeData)
                }
            }
        }


        registerReceiver(scanDataReceiver, IntentFilter("Scan_data_received"))
    }

    private fun getProductIdByBarcode(barcodeData: String) {
        val call = apiBitrix.getProductIdByBarcode(barcodeData)
        call.enqueue(object : Callback<ProductBarcodeResponse> {
            override fun onResponse(call: Call<ProductBarcodeResponse>, response: Response<ProductBarcodeResponse>) {
                if (response.isSuccessful) {
                    val productId = response.body()?.result?.trim()

                    if (!productId.isNullOrEmpty()) {
                        Log.d("ScanActivity", "Product ID received: $productId")
                        getProductById(productId)
                    } else {
                        Log.e("ScanActivity", "Product ID is null or empty")
                    }
                } else {
                    Log.e("ScanActivity", "Failed to get product ID: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ProductBarcodeResponse>, t: Throwable) {
                Log.e("ScanActivity", "Error getting product ID", t)
            }
        })
    }




    private fun getProductById(productId: String) {
        val productRequest = ProductIdRequest(id = productId)
        val call = apiBitrix.getProductById(productRequest)
        call.enqueue(object : Callback<ProductResponse> {
            override fun onResponse(call: Call<ProductResponse>, response: Response<ProductResponse>) {
                if (response.isSuccessful) {
                    val productDetails = response.body()?.result?.get("product") as? LinkedTreeMap<*, *>
                    if (productDetails != null) {
                        val productMapper = ProductMapper()
                        val measureMapper = ProductMeasureMapper()
                        val product = productMapper.mapToProduct(productDetails)

                        addProductToList(measureMapper.setMeasureNameProduct(product))
                        Log.d("Product", "$product")
                    } else {
                        Log.e("ScanActivity", "Product details are null")
                    }
                } else {
                    Log.e("ScanActivity", "Failed to get product details: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                Log.e("ScanActivity", "Error getting product details", t)
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addProductToList(product: Product) {
        if (!product.checkInList(products)) {
            products.add(0, product)
            adapter.notifyDataSetChanged()
        } else {
            showAlertInfo()
        }
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