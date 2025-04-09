package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.DocumentComingAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.service.CatalogDocumentComingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentComingActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var adapter: DocumentComingAdapter
    private var service: CatalogDocumentComingService? = null
    private lateinit var progressBar: ProgressBar
    private val scope = CoroutineScope(Dispatchers.Main)

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_coming)

        progressBar = findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = findViewById(R.id.documentRecycleView)

        setupRecyclerView(recyclerView)
        loadDocuments()
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(SpaceItemDecoration(15))
        adapter = DocumentComingAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDocuments() {
        progressBar.visibility = View.VISIBLE
        service = CatalogDocumentComingService()

        scope.launch {
            try {
                val documents = withContext(Dispatchers.IO) {
                    service?.getDocumentsSuspend()
                }

                documents?.let {
                    Log.d("DocumentActivity", "Получено документов: ${it.size}")
                    adapter.updateData(it)
                }
            } catch (e: Exception) {
                Log.e("DocumentActivity", "Ошибка загрузки документов", e)
                Toast.makeText(
                    this@DocumentComingActivity,
                    "Ошибка загрузки: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    override fun onItemClick(title: String, idDocument: String) {
        val intent = Intent(this, ProductsDocumentComingActivity::class.java).apply {
            putExtra("title", title)
            putExtra("idDocument", idDocument)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}