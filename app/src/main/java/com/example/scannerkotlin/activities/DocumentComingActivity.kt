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
import androidx.lifecycle.lifecycleScope 
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.DocumentComingAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.model.Document 
import com.example.scannerkotlin.service.CatalogDocumentComingService




import kotlinx.coroutines.Job 
import kotlinx.coroutines.isActive 
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers 

@RequiresApi(Build.VERSION_CODES.O) 
class DocumentComingActivity : AppCompatActivity(), OnItemClickListener {

    
    private lateinit var adapter: DocumentComingAdapter
    private lateinit var service: CatalogDocumentComingService
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView 

    private var loadDocumentsJob: Job? = null 

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_coming)

        
        service = CatalogDocumentComingService()

        
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.documentRecycleView)

        
        setupRecyclerView()
        
        loadDocuments()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(SpaceItemDecoration(15)) 
        
        adapter = DocumentComingAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("NotifyDataSetChanged") 
    private fun loadDocuments() {
        
        loadDocumentsJob?.cancel()
        progressBar.visibility = View.VISIBLE
        Log.d("DocumentActivity", "Starting document load...")

        
        loadDocumentsJob = lifecycleScope.launch {
            var documents: List<Document> = emptyList() 
            var error: Throwable? = null

            try {
                
                
                documents = service.getDocumentsSuspend()
                Log.d("DocumentActivity", "Loaded ${documents.size} documents")

            } catch (e: Exception) {
                Log.e("DocumentActivity", "Error loading documents", e)
                error = e 
            } finally {
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (isActive) { 
                        if (error != null) {
                            Toast.makeText(
                                this@DocumentComingActivity,
                                "Ошибка загрузки: ${error.message}",
                                Toast.LENGTH_LONG 
                            ).show()
                            adapter.updateData(emptyList()) 
                        } else {
                            
                            adapter.updateData(documents)
                            
                        }
                    } else {
                        Log.d("DocumentActivity", "Document loading coroutine was cancelled. UI not updated.")
                    }
                }
            }
        }
    }

    
    override fun onItemClick(title: String, idDocument: String) {
        Log.d("DocumentActivity", "Item clicked: Title='$title', ID='$idDocument'")
        val intent = Intent(this, ProductsDocumentComingActivity::class.java).apply {
            putExtra("title", title)
            putExtra("idDocument", idDocument)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        
        
    }
}