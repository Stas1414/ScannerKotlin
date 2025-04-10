package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope 
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.DocumentMovingAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.model.Document 
import com.example.scannerkotlin.service.CatalogDocumentMovingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job 
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O) 
class DocumentMovingActivity : AppCompatActivity(), OnItemClickListener {

    
    private lateinit var adapter: DocumentMovingAdapter
    private lateinit var service: CatalogDocumentMovingService
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAddDocument: Button
    private lateinit var recyclerView: RecyclerView 

    private var loadDocumentsJob: Job? = null 

    @SuppressLint("MissingInflatedId") 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_moving)

        
        service = CatalogDocumentMovingService()

        
        progressBar = findViewById(R.id.progressBar)
        btnAddDocument = findViewById(R.id.btnAddDocumentMoving)
        recyclerView = findViewById(R.id.documentMovingRecycleView)

        
        setupRecyclerView()

        
        setupAddButtonListener()

        
        loadDocuments()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        val space = 15 
        recyclerView.addItemDecoration(SpaceItemDecoration(space))
        
        adapter = DocumentMovingAdapter(mutableListOf())
        recyclerView.adapter = adapter
    }

    private fun setupAddButtonListener() {
        btnAddDocument.setOnClickListener {
            val userId = intent.getStringExtra("userId")
            if (userId.isNullOrBlank()) {
                Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("DocumentMovingActivity", "Add button clicked, userId: $userId")
            addNewDocument(userId) 
        }
    }

    private fun addNewDocument(userId: String) {
        
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE 
            var success = false 
            try {
                Log.d("DocumentMovingActivity", "Calling addNewDocument suspend function...")
                
                success = service.addNewDocument(userId)
                Log.d("DocumentMovingActivity", "addNewDocument returned: $success")

                
                if (success) {
                    Toast.makeText(this@DocumentMovingActivity, "Документ успешно добавлен", Toast.LENGTH_SHORT).show()
                    
                    loadDocuments()
                } else {
                    Toast.makeText(this@DocumentMovingActivity, "Ошибка при добавлении документа", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE 
                }
            } catch (e: Exception) {
                
                Log.e("DocumentMovingActivity", "Error calling addNewDocument", e)
                Toast.makeText(this@DocumentMovingActivity, "Ошибка сети или сервера при добавлении", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE 
            } finally {
                
                
                
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged") 
    private fun loadDocuments() {
        
        loadDocumentsJob?.cancel()

        Log.d("DocumentMovingActivity", "loadDocuments called")
        progressBar.visibility = View.VISIBLE 

        
        loadDocumentsJob = lifecycleScope.launch {
            var documents: List<Document> = emptyList() 
            var error: Exception? = null 

            try {
                Log.d("DocumentMovingActivity", "Calling getDocumentsSuspend...")
                
                
                documents = service.getDocumentsSuspend()
                Log.d("DocumentMovingActivity", "getDocumentsSuspend returned ${documents.size} documents")
            } catch (e: Exception) {
                Log.e("DocumentMovingActivity", "Error loading documents", e)
                error = e 
            } finally {
                
                
                withContext(Dispatchers.Main) { 
                    progressBar.visibility = View.GONE 
                    if (isActive) { 
                        if (error != null) {
                            Toast.makeText(
                                this@DocumentMovingActivity,
                                "Ошибка загрузки документов: ${error.message}",
                                Toast.LENGTH_LONG 
                            ).show()
                            adapter.updateData(emptyList()) 
                        } else {
                            adapter.updateData(documents) 
                            
                            
                            Log.d("DocumentMovingActivity", "Adapter updated with ${documents.size} items.")
                        }
                    } else {
                        Log.d("DocumentMovingActivity", "loadDocuments Job was cancelled, UI not updated.")
                    }
                }
            }
        }
    }

    
    override fun onItemClick(title: String, idDocument: String) {
        Log.d("DocumentMovingActivity", "Item clicked: Title='$title', ID='$idDocument'")
        val intent = Intent(this, ProductsDocumentMovingActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("idDocument", idDocument)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        loadDocumentsJob?.cancel()
        Log.d("DocumentMovingActivity", "onDestroy called, loadDocumentsJob cancelled.")
    }
}