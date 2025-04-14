package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
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
import com.example.scannerkotlin.utils.SessionManager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalStateException

@RequiresApi(Build.VERSION_CODES.O)
class DocumentMovingActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var adapter: DocumentMovingAdapter
    private lateinit var service: CatalogDocumentMovingService
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAddDocument: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyListMovingTextView: TextView

    private var loadDocumentsJob: Job? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (!SessionManager.isUserLoggedIn()) {
            Log.e("DocumentMovingActivity", "User not logged in. Finishing activity.")
            Toast.makeText(this, "Сессия истекла. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_document_moving)

        service = CatalogDocumentMovingService()


        progressBar = findViewById(R.id.progressBar)
        btnAddDocument = findViewById(R.id.btnAddDocumentMoving)
        recyclerView = findViewById(R.id.documentMovingRecycleView)
        emptyListMovingTextView = findViewById(R.id.emptyListMovingTextView)

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
            try {
                val userId = SessionManager.requireUserId()
                Log.d("DocumentMovingActivity", "Add button clicked, userId: $userId")
                addNewDocument(userId)
            } catch (e: IllegalStateException) {
                Log.e("DocumentMovingActivity", "Failed to get userId, user might not be logged in.", e)
                Toast.makeText(this, "Ошибка сессии. Пожалуйста, перезайдите.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun addNewDocument(userId: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE

            recyclerView.visibility = View.GONE
            emptyListMovingTextView.visibility = View.GONE
            var success = false
            try {
                Log.d("DocumentMovingActivity", "Calling addNewDocument suspend function with userId: $userId")
                success = service.addNewDocument(userId)
                Log.d("DocumentMovingActivity", "addNewDocument returned: $success")

                if (success) {
                    Toast.makeText(this@DocumentMovingActivity, "Документ успешно добавлен", Toast.LENGTH_SHORT).show()
                    loadDocuments()
                } else {
                    Toast.makeText(this@DocumentMovingActivity, "Не удалось добавить документ (ответ сервера)", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE

                     loadDocuments()
                     if (adapter.itemCount > 0) recyclerView.visibility = View.VISIBLE else emptyListMovingTextView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("DocumentMovingActivity", "Error calling addNewDocument", e)
                Toast.makeText(this@DocumentMovingActivity, "Ошибка сети или сервера при добавлении", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE

                 if (adapter.itemCount > 0) recyclerView.visibility = View.VISIBLE else emptyListMovingTextView.visibility = View.VISIBLE
            }

        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun loadDocuments() {
        loadDocumentsJob?.cancel()

        Log.d("DocumentMovingActivity", "loadDocuments called")
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyListMovingTextView.visibility = View.GONE

        loadDocumentsJob = lifecycleScope.launch {
            var documents: List<Document> = emptyList()
            var error: Exception? = null

            try {
                Log.d("DocumentMovingActivity", "Calling getDocumentsSuspend...")

                documents = withContext(Dispatchers.IO) {
                    service.getDocumentsSuspend()
                }
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
                            emptyListMovingTextView.text = "Ошибка загрузки"
                            emptyListMovingTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE
                            adapter.updateData(emptyList())
                        } else {

                            if (documents.isEmpty()) {

                                Log.d("DocumentMovingActivity", "Document list is empty.")
                                emptyListMovingTextView.text = "Список пуст"
                                emptyListMovingTextView.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                            } else {

                                Log.d("DocumentMovingActivity", "Document list is NOT empty.")
                                emptyListMovingTextView.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                adapter.updateData(documents)
                            }
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
        val intent = Intent(this, ProductsDocumentMovingActivity::class.java).apply {
            putExtra("title", title)
            putExtra("idDocument", idDocument)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        loadDocumentsJob?.cancel()
        Log.d("DocumentMovingActivity", "onDestroy called, loadDocumentsJob cancelled.")
    }
}