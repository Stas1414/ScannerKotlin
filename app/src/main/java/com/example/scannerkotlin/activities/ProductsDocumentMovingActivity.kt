package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductMovingAdapter
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.DocumentElementMapper
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.mappers.ProductToDocumentElementMapper
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Store
import com.example.scannerkotlin.request.ProductIdRequest
import com.example.scannerkotlin.response.ProductBarcodeResponse
import com.example.scannerkotlin.response.ProductResponse
import com.example.scannerkotlin.service.CatalogDocumentMovingService
import com.google.gson.internal.LinkedTreeMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductsDocumentMovingActivity : AppCompatActivity() {

    private var scanDataReceiver: BroadcastReceiver? = null

    private val baseUrl = "https://bitrix.izocom.by/rest/1/c953o6imkob2gpwd/"
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiBitrix: ApiBitrix = retrofit.create(ApiBitrix::class.java)

    private val elements: MutableList<DocumentElement> = mutableListOf()
    private val baseList:MutableList<DocumentElement> = mutableListOf()
    private val deletedProduct:MutableList<DocumentElement> = mutableListOf()
    private val newProductInDocument:MutableList<DocumentElement> = mutableListOf()

    private lateinit var adapter:ProductMovingAdapter

    private lateinit var progressBar: ProgressBar

    private val service by lazy { CatalogDocumentMovingService() }

    private var storeList:MutableList<Store> = mutableListOf()

    private lateinit var recyclerView: RecyclerView




    @SuppressLint("UnspecifiedRegisterReceiverFlag", "MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_document_moving)

        recyclerView = findViewById(R.id.rvMovingProducts)
        progressBar = findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ProductMovingAdapter(this, mutableListOf(), mutableListOf()) { position ->
            adapter.removeItem(position)
        }
        recyclerView.adapter = adapter

        loadProducts()

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



    @SuppressLint("NotifyDataSetChanged")
    private fun loadProducts() {
        progressBar.visibility = View.VISIBLE
        val idDocument = intent.getStringExtra("idDocument")?.toIntOrNull() ?: return

        try {

            service.getStoreList { stores ->
                storeList.clear()
                storeList.addAll(stores)


                service.performDocumentElementsRequest(idDocument) { products ->
                    Log.d("ProductsActivity", "Loaded products: ${products.size}")

                    runOnUiThread {
                        if (!isFinishing) {
                            elements.clear()
                            elements.addAll(products)
                            baseList.addAll(products)


                            adapter = ProductMovingAdapter(
                                this@ProductsDocumentMovingActivity,
                                elements,
                                storeList
                            ) { position ->
                                adapter.removeItem(position)
                                deletedProduct.add(elements[position])
                                elements.removeAt(position)
                            }
                            recyclerView.adapter = adapter

                            recyclerView.recycledViewPool.clear()
                            adapter.notifyDataSetChanged()
                        }
                        progressBar.visibility = View.GONE
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("ProductsActivity", "Error loading products", e)
            progressBar.visibility = View.GONE
        }
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
                        showAlertInfo("Товар не найден")
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
                        val mapper = ProductToDocumentElementMapper()
                        val productMapper = ProductMapper()
                        val product = productMapper.mapToProduct(productDetails)
                        val element = mapper.map(product, productId.toLong())
                        showAlertScanInfo(element, onAddProduct = {
                            addProductToList(element)
                        })
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
    private fun addProductToList(element: DocumentElement) {
        if (!element.checkInList(elements)) {
            elements.add(0, element)
            newProductInDocument.add(element)
            adapter.notifyDataSetChanged()

            recyclerView.recycledViewPool.clear()
            adapter.notifyDataSetChanged()
        } else {
            showAlertInfo("Этот товар уже отсканирован")
        }
    }

    private fun showAlertInfo(text: String) {
        AlertDialog.Builder(this).apply {
            setTitle("Предупреждение")
            setMessage(text)
            setCancelable(false)
            setPositiveButton("Закрыть") { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

    private fun findNameOfStore(idStore: Int?): String?{
        if (idStore != null) {
            for (store in storeList) {
                if (idStore == store.id) {
                    return store.title
                }
            }
        }
        return null

    }


    private fun showAlertScanInfo(element: DocumentElement, onAddProduct: () -> Unit) {
        val storeName = findNameOfStore(element.storeFrom)
//        if (storeName == null) {
//            showAlertInfo("Название склада null")
//            return
//        }
        AlertDialog.Builder(this).apply {
            setTitle("Информация о товаре")
            setMessage("${element.name} \n${element.amount} \n$storeName")
            setCancelable(false)
            setPositiveButton("Добавить товар") { dialog, _ ->
                onAddProduct()
                dialog.dismiss()
            }
            setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun areAllFieldsFilled(): Boolean {
        return productList.all { product ->
            (product.quantity ?: 0) > 0 &&
                    !product.barcode.isNullOrEmpty()
        }
    }

    fun updateSaveButtonState() {
        val isAllFieldsFilled = areAllFieldsFilled()
        btnSave?.isEnabled = isAllFieldsFilled
        btnSave?.setBackgroundColor(
            if (isAllFieldsFilled) {
                ContextCompat.getColor(this, R.color.blue)
            } else {
                ContextCompat.getColor(this, R.color.gray)
            }

        )
    }



    override fun onDestroy() {
        super.onDestroy()

        if (scanDataReceiver != null) {
            unregisterReceiver(scanDataReceiver)
        }
    }
}