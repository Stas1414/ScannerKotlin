package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductAdapter
import com.example.scannerkotlin.mappers.ProductMeasureMapper
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.ProductOffer
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.service.CatalogService
import java.time.LocalDateTime

class ProductsDocumentActivity : AppCompatActivity() {

    private var btnSave: Button? = null
    private var btnAddProduct: Button? = null
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val service by lazy { CatalogService() }
    private var productOffersList = mutableListOf<ProductOffer>()
    private var barcodesMap = mutableMapOf<Int, String>()

    private var scanDataReceiver: BroadcastReceiver? = null

    private lateinit var progressBar: ProgressBar

    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_products)



        btnSave = findViewById(R.id.btnSave)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        progressBar = findViewById(R.id.progressBar)


        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == "Scan_data_received") {
                    val barcodeData = intent.getStringExtra("scanData").toString()
                    Log.d("ProductActivity", "Scanned barcode: $barcodeData")

                    runOnUiThread {

                        val productId = adapter.updateFocusedProductBarcode(barcodeData)
                        if (productId != null) {
                            Log.d("Adapter", "Обновлен продукт с ID: $productId")
                        } else {
                            Log.e("Adapter", "Не удалось обновить штрихкод, нет фокусированного продукта")
                        }
                    }
                }
            }
        }
        registerReceiver(scanDataReceiver, IntentFilter("Scan_data_received"))


        setupRecyclerView()
        setupUI()
        loadProducts()

        val idDocument: Int? = intent.getStringExtra("idDocument")?.toInt()

        btnSave?.setOnClickListener {
            service.saveProductOffers(productOffersList) { isLoading ->
                if (isLoading) {
                    progressBar.visibility = View.VISIBLE
                    Log.d("SaveProductOffers", "Сохранение началось...")
                } else {
                    progressBar.visibility = View.GONE
                    Log.d("SaveProductOffers", "Сохранение завершено!")
                }
            }
        }

        btnAddProduct?.setOnClickListener {
            addProduct()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createProductOffer(product: Product): ProductOffer{
        val productOffer = ProductOffer(product)
        return productOffer
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged")
    private fun addProduct() {
        val productRequest = ProductRequest(
            filter = mapOf("iblockId" to 14)
        )

        service.performFinalRequest(productRequest, onComplete = { products ->
            if (products.isEmpty()) {
                showAlert("Нет доступных продуктов", emptyList()) {}
            } else {
                showAlert("Выберите продукт", products) { selectedProduct ->
                    if (selectedProduct != null) {
                        Toast.makeText(this, "Вы выбрали: ${selectedProduct.name}", Toast.LENGTH_SHORT).show()
                        productList.add(0, selectedProduct)
                        productOffersList.add(createProductOffer(selectedProduct))
                        adapter.notifyItemInserted(0)
                        updateSaveButtonState()
                    } else {
                        Toast.makeText(this, "Выбор отменён", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showAlert(
        title: String,
        products: List<Product>,
        onProductSelected: (Product?) -> Unit
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val productNames = products.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productNames)

        val listView = ListView(this)
        listView.adapter = adapter

        builder.setView(listView)
        builder.setNegativeButton("Закрыть") { dialog, _ ->
            onProductSelected(null)
            dialog.dismiss()
        }

        val dialog = builder.create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedProduct = products[position]
            onProductSelected(selectedProduct)
            dialog.dismiss()
        }

        dialog.show()
    }





    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.rvProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ProductAdapter(productList) { position ->
            productList.removeAt(position)
            adapter.notifyItemRemoved(position)
            updateSaveButtonState()
        }

        recyclerView.adapter = adapter
    }

    private fun setupUI() {

        val title = intent.getStringExtra("title") ?: "Unknown Title"
        val idDocument = intent.getStringExtra("idDocument")?.toIntOrNull()

        Log.d("ProductsActivity", "Title: $title")
        Log.d("ProductsActivity", "Document ID: $idDocument")

        findViewById<TextView>(R.id.mainTitle).text = title
        supportActionBar?.title = title

        if (idDocument == null) {
            Log.e("ProductsActivity", "idDocument is null, cannot load products")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadProducts() {
        progressBar.visibility = View.VISIBLE
        val idDocument = intent.getStringExtra("idDocument")?.toIntOrNull() ?: return

        try {
            service.performDocumentElementsRequest(idDocument) { products ->
                Log.d("ProductsActivity", "Loaded products: ${products.size}")

                runOnUiThread {
                    if (!isFinishing) {
                        productList.clear()
                        productList.addAll(products)
                        adapter.notifyDataSetChanged()
                    }

                    progressBar.visibility = View.GONE
                }
            }

        } catch (e: Exception) {
            Log.e("ProductsActivity", "Error loading products", e)
            progressBar.visibility = View.GONE
        }
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

        unregisterReceiver(scanDataReceiver)
    }
}