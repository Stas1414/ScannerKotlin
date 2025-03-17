package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.ProductAdapter
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.service.CatalogService

class ProductsActivity : AppCompatActivity() {

    private lateinit var adapter: ProductAdapter
    private var service: CatalogService? = null
    private val productList = mutableListOf<Product>()

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        val recyclerView: RecyclerView = findViewById(R.id.recycleViewProducts)
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = ProductAdapter(productList)
        recyclerView.adapter = adapter

        val title = intent.getStringExtra("title") ?: "Unknown Title"
        val idDocument: Int? = intent.getStringExtra("idDocument")?.toIntOrNull()

        Log.d("ProductActivity->title", title)
        Log.d("ProductActivity->idDocument", idDocument.toString())

        val titleTextView: TextView = findViewById(R.id.DocumentProducts)
        titleTextView.text = title

        supportActionBar?.title = title

        service = CatalogService()

        idDocument?.let { docId ->
            service?.performDocumentElementsRequest(docId) { products ->
                Log.d("Check final size products", products.size.toString())
                runOnUiThread {
                    productList.clear()
                    productList.addAll(products)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}
