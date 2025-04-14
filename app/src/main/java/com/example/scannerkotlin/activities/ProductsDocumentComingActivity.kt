package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductComingAdapter
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.ProductOffer
import com.example.scannerkotlin.service.CatalogDocumentComingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@RequiresApi(Build.VERSION_CODES.O)
class ProductsDocumentComingActivity : AppCompatActivity() {


    private lateinit var btnSave: Button
    private lateinit var btnAddProduct: Button
    private lateinit var adapter: ProductComingAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var service: CatalogDocumentComingService
    private lateinit var emptyProductListTextView: TextView

    private lateinit var productMapper: ProductMapper


    private val baseList = mutableListOf<Product>()
    private val productList = mutableListOf<Product>()
    private val productOffersList = mutableListOf<ProductOffer>()
    private val deletedProductsList = mutableListOf<Product>()


    private var scanDataReceiver: BroadcastReceiver? = null


    private var idDocument: Int? = null



    @SuppressLint("UnspecifiedRegisterReceiverFlag", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_products)

        idDocument = intent.getStringExtra("idDocument")?.toIntOrNull()
        if (idDocument == null) {
            Log.e("ProductsComingActivity", "Invalid or missing 'idDocument' extra.")
            Toast.makeText(this, "Ошибка: Неверный ID документа", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeDependencies()
        initializeViews()
        setupRecyclerView()
        setupUI()
        setupButtonClickListeners()
        setupBroadcastReceiver()

        loadProducts()
        updateSaveButtonState()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanDataReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d("ProductsComingActivity", "Scan data receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w("ProductsComingActivity", "Receiver not registered?", e)
            }
        }
        scanDataReceiver = null
    }



    private fun initializeDependencies() {
        service = CatalogDocumentComingService()
        productMapper = ProductMapper()

    }

    private fun initializeViews() {
        btnSave = findViewById(R.id.btnSave)
        btnAddProduct = findViewById(R.id.btnAddProduct)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.rvProducts)
        emptyProductListTextView = findViewById(R.id.emptyProductListTextView)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ProductComingAdapter(productList) { position ->
            handleItemDeletion(position)
        }

        adapter.setOnDataChangedListener {
            updateSaveButtonState()
        }
        recyclerView.adapter = adapter
    }

    private fun setupUI() {
        val title = intent.getStringExtra("title") ?: "Приход товара"
        findViewById<TextView>(R.id.mainTitle).text = title
        supportActionBar?.title = title
        Log.d("ProductsComingActivity", "UI setup: Title='$title', ID='$idDocument'")
    }

    private fun setupButtonClickListeners() {
        btnSave.setOnClickListener { handleSaveButtonClick() }
        btnAddProduct.setOnClickListener { handleAddProductClick() }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupBroadcastReceiver() {
        scanDataReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "Scan_data_received") {
                    val barcodeData = intent.getStringExtra("scanData")
                    if (!barcodeData.isNullOrBlank()) {
                        Log.d("ProductsComingActivity", "Scanned barcode: $barcodeData")
                        handleBarcodeScanResult(barcodeData)
                    }
                }
            }
        }

        val filter = IntentFilter("Scan_data_received")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanDataReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(scanDataReceiver, filter)
        }
        Log.d("ProductsComingActivity", "Scan data receiver registered")
    }



    @SuppressLint("NotifyDataSetChanged")
    private fun loadProducts() {
        val currentIdDocument = idDocument ?: return
        progressBar.visibility = View.VISIBLE
        emptyProductListTextView.visibility = View.GONE
        Log.d("ProductsComingActivity", "Starting product load for document $currentIdDocument")

        lifecycleScope.launch {
            var loadedProducts: List<Product> = emptyList()
            var error: Throwable? = null

            try {
                loadedProducts = service.getDocumentProductsWithDetails(currentIdDocument)
                Log.d("ProductsComingActivity", "Loaded ${loadedProducts.size} products from service")
            } catch (e: Exception) {
                Log.e("ProductsComingActivity", "Error loading products", e)
                error = e
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (isActive) {
                        if (error == null) {
                            productList.clear()
                            productList.addAll(loadedProducts)
                            baseList.clear()
                            baseList.addAll(loadedProducts)
                            adapter.notifyDataSetChanged()
                            updateSaveButtonState()
                            updateEmptyStateVisibility()
                        } else {
                            Toast.makeText(this@ProductsDocumentComingActivity, "Ошибка загрузки продуктов: ${error.message}", Toast.LENGTH_LONG).show()
                            productList.clear()
                            baseList.clear()
                            adapter.notifyDataSetChanged()
                            updateSaveButtonState()
                            updateEmptyStateVisibility()
                        }
                    }
                }
            }
        }
    }


    private fun handleAddProductClick() {
        Log.d("ProductsComingActivity", "Add product button clicked")
        progressBar.visibility = View.VISIBLE
        btnAddProduct.isEnabled = false

        lifecycleScope.launch {
            var productsForSelection: List<Product> = emptyList()
            var error: Throwable? = null
            val iblockId = 14
            try {
                productsForSelection = service.getProductsForSelection(iblockId) // Используем сервис
                Log.d("ProductsComingActivity", "Loaded ${productsForSelection.size} products for selection via service")
            } catch (e: Exception) {
                Log.e("ProductsComingActivity", "Error getting products for selection via service", e)
                error = e
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnAddProduct.isEnabled = true
                    if (isActive) {
                        if (error == null) {
                            if (productsForSelection.isEmpty()) {
                                showAlert("Нет доступных продуктов", emptyList()) {}
                            } else {
                                showAlert("Выберите продукт", productsForSelection) { selectedProduct ->
                                    handleProductSelection(selectedProduct)
                                }
                            }
                        } else {
                            Toast.makeText(this@ProductsDocumentComingActivity, "Ошибка загрузки списка продуктов: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun handleProductSelection(selectedProduct: Product?) {
        if (selectedProduct != null) {
            Toast.makeText(this, "Вы выбрали: ${selectedProduct.name}", Toast.LENGTH_SHORT).show()
            if (productList.none { it.id == selectedProduct.id }) {
                productList.add(0, selectedProduct)
                Log.d("ProductsComingActivity", "Product ${selectedProduct.name} added to productList (${productList.size})")
                val offer = createProductOffer(selectedProduct)
                productOffersList.add(offer)
                Log.d("ProductsComingActivity", "Product ${selectedProduct.name} added to productOfferList (${productOffersList.size})")
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
                updateEmptyStateVisibility()
                updateSaveButtonState()
            } else {
                Toast.makeText(this, "Товар '${selectedProduct.name}' уже есть в списке", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Выбор отменён", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createProductOffer(product: Product): ProductOffer {
        return ProductOffer(product)
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun handleItemDeletion(position: Int) {
        if (position < 0 || position >= productList.size) return
        val removedProduct = productList.removeAt(position)
        Log.d("ProductsComingActivity", "Removed product: ${removedProduct.name} at position $position")
        if (baseList.any { it.idInDocument == removedProduct.idInDocument && removedProduct.idInDocument != null }) {
            deletedProductsList.add(removedProduct)
            Log.d("ProductsComingActivity", "Added to deletedProductsList. Size: ${deletedProductsList.size}")
        }
        val removedOffer = productOffersList.removeAll { it.product.id == removedProduct.id }
        if (removedOffer) {
            Log.d("ProductsComingActivity", "Removed from productOffersList.")
        }
        adapter.notifyDataSetChanged()
        updateSaveButtonState()
        updateEmptyStateVisibility()
    }


    private fun handleSaveButtonClick() {
        val currentIdDocument = idDocument ?: return
        if (!areAllFieldsFilled()) {
            Toast.makeText(this, "Заполните количество (> 0) и штрихкод для всех товаров", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("ProductsComingActivity", "Save button clicked for document $currentIdDocument")
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        val productsActuallyUpdated = productList.filter { current ->
            val base = baseList.find { it.idInDocument == current.idInDocument }
            base != null &&
                    (current.quantity != base.quantity || current.barcode != base.barcode || current.measureId != base.measureId) &&
                    !deletedProductsList.contains(current)
        }
        Log.d("ProductsComingActivity", "Products actually updated: ${productsActuallyUpdated.size}")

        lifecycleScope.launch {
            var success = false
            var error: Throwable? = null

            try {
                Log.d("ProductsComingActivity", "Calling conductDocumentSuspend: base=${baseList.size}, deleted=${deletedProductsList.size}, updatedActual=${productsActuallyUpdated.size}, newOffers=${productOffersList.size}")
                success = service.conductDocumentSuspend(
                    baseList = baseList,
                    idDocument = currentIdDocument,
                    context = this@ProductsDocumentComingActivity,
                    deletedProducts = deletedProductsList,
                    updatedProducts = productsActuallyUpdated,
                    currentProductListState = productList, // Передаем актуальный список
                    productOffersList = productOffersList
                )
                Log.d("ProductsComingActivity", "conductDocumentSuspend returned: $success")
            } catch (e: Exception) {
                Log.e("ProductsComingActivity", "Error calling conductDocumentSuspend", e)
                error = e
                success = false
            } finally {

                withContext(Dispatchers.Main) {

                    progressBar.visibility = View.GONE

                    if (isActive) {
                        if (success) {

                            Log.d("ProductsComingActivity", "Conduct successful, navigating to MainActivity.")
                            Toast.makeText(this@ProductsDocumentComingActivity, "Документ успешно проведен", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@ProductsDocumentComingActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                        } else {

                            btnSave.isEnabled = true
                            if (error != null) {

                                Toast.makeText(this@ProductsDocumentComingActivity, "Ошибка проведения: ${error.message}", Toast.LENGTH_LONG).show()
                            } else {

                                Log.w("ProductsComingActivity", "Conduct document reported failure, but no exception was caught here.")
                            }
                        }
                    } else {
                        Log.w("ProductsComingActivity", "Coroutine cancelled before UI update in finally block.")
                    }
                }
            }
        }
    }


    private fun handleBarcodeScanResult(barcode: String) {
        Log.d("ProductsComingActivity", "Handling barcode scan result: $barcode")
        val success = adapter.updateFocusedProductBarcode(barcode)
        if (success) {
            Log.d("ProductsComingActivity", "Barcode updated in adapter.")
            Toast.makeText(this, "Штрихкод '$barcode' установлен", Toast.LENGTH_SHORT).show()
            updateSaveButtonState()
        } else {
            Log.w("ProductsComingActivity", "Failed to update barcode in adapter (no focused product?).")
            Toast.makeText(this, "Сначала выберите поле Штрихкод у товара", Toast.LENGTH_LONG).show()
        }
    }



    private fun showAlert(
        title: String,
        products: List<Product>,
        onProductSelected: (Product?) -> Unit
    ) {
        if (!isFinishing) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            if (products.isEmpty()) {
                builder.setMessage("Нет доступных вариантов.")
                builder.setPositiveButton("OK") { dialog, _ ->
                    onProductSelected(null)
                    dialog.dismiss()
                }
            } else {
                val productNames = products.map { it.name ?: "Без имени" }
                val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productNames)
                builder.setAdapter(listAdapter) { dialog, which ->
                    val selectedProduct = products[which]
                    onProductSelected(selectedProduct)
                    dialog.dismiss()
                }
                builder.setNegativeButton("Отмена") { dialog, _ ->
                    onProductSelected(null)
                    dialog.dismiss()
                }
            }
            builder.create().show()
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        if (productList.isEmpty()) return false
        return productList.all { product ->
            (product.quantity ?: 0) > 0 &&
                    !product.barcode.isNullOrBlank()
        }
    }

    private fun updateSaveButtonState() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { updateSaveButtonStateInternal() }
        } else {
            updateSaveButtonStateInternal()
        }
    }

    private fun updateSaveButtonStateInternal() {
        val canSave = areAllFieldsFilled()
        Log.d("ProductsComingActivity", "Updating save button state. CanSave: $canSave")
        btnSave.isEnabled = canSave
        btnSave.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (canSave) R.color.blue else R.color.gray
            )
        )
    }

    private fun updateEmptyStateVisibility() {
        if (productList.isEmpty()) {
            emptyProductListTextView.text = "Список товаров пуст"
            emptyProductListTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyProductListTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}