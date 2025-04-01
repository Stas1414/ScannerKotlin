package com.example.scannerkotlin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scannerkotlin.R
import com.example.scannerkotlin.adapter.DocumentComingAdapter
import com.example.scannerkotlin.adapter.DocumentMovingAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.service.CatalogDocumentMovingService

class DocumentMovingActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var adapter: DocumentMovingAdapter
    private var service: CatalogDocumentMovingService? = null
    private lateinit var progressBar: ProgressBar

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_moving)


        progressBar = findViewById(R.id.progressBar)

        val recyclerView: RecyclerView = findViewById(R.id.documentMovingRecycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val space = 15
        recyclerView.addItemDecoration(SpaceItemDecoration(space))

        service = CatalogDocumentMovingService()

        adapter = DocumentMovingAdapter(mutableListOf())
        recyclerView.adapter = adapter


        service?.performDocumentListRequest(
            onComplete = { documents ->
                runOnUiThread {
                    Log.d("DocumentActivity", "Получено документов: ${documents.size}")


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


        progressBar.visibility = View.VISIBLE
    }

    override fun onItemClick(title: String, idDocument: String) {
        val intent = Intent(this, ProductsDocumentMovingActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("idDocument", idDocument)
        startActivity(intent)
    }
}