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
import com.example.scannerkotlin.adapter.ProductComingAdapter
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.ProductOffer
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.service.CatalogDocumentComingService

class ProductsDocumentComingActivity : AppCompatActivity() {

    private var btnSave: Button? = null
    private var btnAddProduct: Button? = null
    private lateinit var adapter: ProductComingAdapter
    private val baseList = mutableListOf<Product>()
    private val productList = mutableListOf<Product>()
    private val service by lazy { CatalogDocumentComingService() }
    private var productOffersList = mutableListOf<ProductOffer>()
    private var deletedProductsList = mutableListOf<Product>()

    private var scanDataReceiver: BroadcastReceiver? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView


    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_products)



        btnSave = findViewById(R.id.btnSave)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.rvProducts)


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
            service.conductDocument(
                baseList,
                idDocument,
                deletedProducts = deletedProductsList,
                context = this,
                updatedProducts = productList,
                productOffersList = productOffersList,
                onLoading = { isLoading ->
                    progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                },
                callback = { success ->
                    if (success) {
                        Toast.makeText(this, "Документ успешно проведен", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Ошибка при проведении документа", Toast.LENGTH_SHORT).show()
                    }
                }
            )
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
                        Log.d("Activity", "Product ${selectedProduct.name} добавлен в productList      (${productList.size})")
                        productOffersList.add(createProductOffer(selectedProduct))
                        Log.d("Activity", "Product ${selectedProduct.name} добавлен в productOfferList   (${productOffersList.size})")
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





    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductComingAdapter(productList) { position ->
            if (position in productList.indices) {
                val removedProduct = productList.removeAt(position)
                deletedProductsList.add(removedProduct)
                productOffersList.removeAll { it.product == removedProduct }

                Log.d("Activity", "Удалён продукт: ${removedProduct.name}, осталось ${productList.size} элементов")

                if (productList.isEmpty()) {
                    adapter.notifyDataSetChanged()
                } else {
                    adapter.notifyItemRemoved(position)
                    adapter.notifyItemRangeChanged(position, productList.size)
                }

                updateSaveButtonState()
            }
        }

        recyclerView.adapter = adapter

        recyclerView.recycledViewPool.clear()
        adapter.notifyDataSetChanged()
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
                        baseList.addAll(products)


                        recyclerView.recycledViewPool.clear()
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