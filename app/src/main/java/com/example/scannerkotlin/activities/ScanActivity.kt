package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductScanAdapter
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.mappers.ProductMeasureMapper
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.request.ProductIdRequest
import com.example.scannerkotlin.response.ProductResponse
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class ScanActivity : AppCompatActivity() {

    private val products: MutableList<Product> = mutableListOf()
    private lateinit var adapter: ProductScanAdapter
    private var scanDataReceiver: BroadcastReceiver? = null


    private val apiBitrix: ApiBitrix by lazy {
        Retrofit.Builder()
            .baseUrl("https://bitrix.izocom.by/rest/1/c953o6imkob2gpwd/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiBitrix::class.java)
    }


    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("ScanActivityCoroutine", "Coroutine exception", throwable)

    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activity)

        setupRecyclerView()
        setupBroadcastReceiver()
        registerScanReceiver()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycleViewScan)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductScanAdapter(products)
        recyclerView.adapter = adapter
    }

    private fun setupBroadcastReceiver() {
        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "Scan_data_received") {
                    val barcodeData = intent.getStringExtra("scanData")
                    if (!barcodeData.isNullOrBlank()) {
                        Log.d("ScanActivity", "Scanned barcode: $barcodeData")
                        processBarcode(barcodeData)
                    } else {
                        Log.w("ScanActivity", "Received null or blank scanData")
                    }
                }
            }
        }
    }


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerScanReceiver() {
        scanDataReceiver?.let { receiver ->
            val filter = IntentFilter("Scan_data_received")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            Log.d("ScanActivity", "Scan data receiver registered")
        }
    }


    private fun processBarcode(barcode: String) {

        lifecycleScope.launch(coroutineExceptionHandler) {
            try {

                val productId = getProductIdByBarcodeSuspend(barcode)

                if (!productId.isNullOrEmpty()) {
                    Log.d("ScanActivity", "Product ID received: $productId")

                    val product = getProductByIdSuspend(productId)


                    if (product != null) {
                        withContext(Dispatchers.Main) {
                            addProductToList(product)
                        }
                    } else {

                        withContext(Dispatchers.Main) {
                            showAlertInfo("Не удалось получить детали товара ID: $productId")
                        }
                    }
                } else {

                    withContext(Dispatchers.Main) {
                        showAlertInfo("Товар по штрихкоду '$barcode' не найден")
                    }
                }
            } catch (e: IOException) {
                Log.e("ScanActivity", "Network or IO Error processing barcode $barcode", e)
                withContext(Dispatchers.Main) {
                    showAlertInfo("Ошибка сети при обработке штрихкода")
                }
            } catch (e: Exception) {
                Log.e("ScanActivity", "Unexpected error processing barcode $barcode", e)
                withContext(Dispatchers.Main) {
                    showAlertInfo("Произошла ошибка при обработке штрихкода")
                }
            }
        }
    }


    private suspend fun getProductIdByBarcodeSuspend(barcodeData: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getProductIdByBarcode(barcodeData)
                if (response.isSuccessful) {
                    val productId = response.body()?.result?.trim()
                    if (!productId.isNullOrEmpty()) {
                        productId
                    } else {
                        Log.w("ScanActivity", "API returned success but Product ID is null or empty for barcode $barcodeData")
                        null
                    }
                } else {
                    Log.e("ScanActivity", "Failed to get product ID: ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {

                Log.e("ScanActivity", "Error in getProductIdByBarcodeSuspend", e)
                throw e
            }
        }
    }


    private suspend fun getProductByIdSuspend(productId: String): Product? {

        return withContext(Dispatchers.IO) {
            try {
                val productRequest = ProductIdRequest(id = productId)
                val response = apiBitrix.getProductById(productRequest)

                if (response.isSuccessful) {

                    val product = mapProductResponse(response.body())
                    if (product != null) {
                        Log.d("ScanActivity", "Product details fetched: $product")
                        product
                    } else {
                        Log.e("ScanActivity", "Product details mapping failed or result is null for ID $productId")
                        null
                    }
                } else {
                    Log.e("ScanActivity", "Failed to get product details: ${response.code()} - ${response.errorBody()?.string()}")
                    null 
                }
            } catch (e: Exception) {
                Log.e("ScanActivity", "Error in getProductByIdSuspend", e)
                throw e
            }
        }
    }


    private fun mapProductResponse(responseBody: ProductResponse?): Product? {
        return try {
            val productDetails = responseBody?.result?.get("product") as? LinkedTreeMap<*, *>
            if (productDetails != null) {
                val productMapper = ProductMapper()
                val measureMapper = ProductMeasureMapper()
                val product = productMapper.mapToProduct(productDetails)
                measureMapper.setMeasureNameProduct(product) 
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ScanActivity", "Error mapping product response", e)
            null 
        }
    }



    @SuppressLint("NotifyDataSetChanged")
    private fun addProductToList(product: Product) {
        if (!product.checkInList(products)) {
            products.add(0, product)
            adapter.notifyDataSetChanged()
        } else {
            showAlertInfo("Этот товар уже отсканирован")
        }
    }


    private fun showAlertInfo(text: String) {
        if (!isFinishing && !isDestroyed) {
            AlertDialog.Builder(this).apply {
                setTitle("Информация")
                setMessage(text)
                setCancelable(true)
                setPositiveButton("ОК") { dialog, _ ->
                    dialog.dismiss()
                }
            }.create().show()
        } else {
            Log.w("ScanActivity", "Attempted to show alert dialog on finishing/destroyed activity")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        scanDataReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d("ScanActivity", "Scan data receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w("ScanActivity", "Receiver not registered or already unregistered", e)
            }
        }
        scanDataReceiver = null
    }
}