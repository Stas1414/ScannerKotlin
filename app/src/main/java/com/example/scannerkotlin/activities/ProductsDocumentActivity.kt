package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductAdapter
import com.example.scannerkotlin.mappers.ProductMeasureMapper
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.service.CatalogService

class ProductsDocumentActivity : AppCompatActivity() {

    private var btnSave: Button? = null
    private var btnAddProduct: Button? = null
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val service by lazy { CatalogService() }

    private var scanDataReceiver: BroadcastReceiver? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)



        btnSave = findViewById(R.id.btnSave)
        btnAddProduct = findViewById(R.id.btnAddProduct)


        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == "Scan_data_received") {
                    val barcodeData = intent.getStringExtra("scanData").toString()
                    Log.d("ProductActivity", "Scanned barcode: $barcodeData")

                    runOnUiThread {

                        adapter.updateFocusedProductBarcode(barcodeData)
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
            service.conductDocument(idDocument = idDocument, context = this) { success ->
                if (success) {
                    val intent = Intent(this,
                        DocumentActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        btnAddProduct?.setOnClickListener {

        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.rvProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ProductAdapter(productList) { position ->
            productList.removeAt(position)
            adapter.notifyItemRemoved(position)
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
        val idDocument = intent.getStringExtra("idDocument")?.toIntOrNull() ?: return

        try {
            service.performDocumentElementsRequest(idDocument) { products ->
                Log.d("ProductsActivity", "Loaded products: ${products.size}")

                runOnUiThread {
                    if (!isFinishing) {
                        val productMeasureMapper = ProductMeasureMapper(products)
                        productMeasureMapper.setMeasureNameList()

                        productList.clear()
                        productList.addAll(productMeasureMapper.products)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ProductsActivity", "Error loading products", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(scanDataReceiver)
    }
}