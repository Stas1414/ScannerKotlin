package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.DocumentMovingAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.service.CatalogDocumentMovingService

class DocumentMovingActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var adapter: DocumentMovingAdapter
    private var service: CatalogDocumentMovingService? = null
    private lateinit var progressBar: ProgressBar
    private var btnAddDocument:Button? = null

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_moving)


        progressBar = findViewById(R.id.progressBar)
        btnAddDocument = findViewById(R.id.btnAddDocumentMoving)

        btnAddDocument?.setOnClickListener {
            val userId = intent.getStringExtra("userId")
            Log.d("DocumentMovingActivity", "userId: $userId")
            progressBar.visibility = View.VISIBLE

            service?.addNewDocument(userId) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Документ успешно добавлен", Toast.LENGTH_SHORT).show()
                        loadDocuments()
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Ошибка при добавлении документа", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val recyclerView: RecyclerView = findViewById(R.id.documentMovingRecycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val space = 15
        recyclerView.addItemDecoration(SpaceItemDecoration(space))

        service = CatalogDocumentMovingService()

        adapter = DocumentMovingAdapter(mutableListOf())
        recyclerView.adapter = adapter

        loadDocuments()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun loadDocuments() {
        progressBar.visibility = View.VISIBLE

        service?.performDocumentListRequest(
            onComplete = { documents ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    adapter.updateData(documents)
                    adapter.notifyDataSetChanged()
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onLoading = { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        )
    }

    override fun onItemClick(title: String, idDocument: String) {
        val intent = Intent(this, ProductsDocumentMovingActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("idDocument", idDocument)
        startActivity(intent)
    }

}