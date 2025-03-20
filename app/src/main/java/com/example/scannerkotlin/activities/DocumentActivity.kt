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
import com.example.scannerkotlin.adapter.DocumentAdapter
import com.example.scannerkotlin.decoration.SpaceItemDecoration
import com.example.scannerkotlin.listener.OnItemClickListener
import com.example.scannerkotlin.service.CatalogService

class DocumentActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var adapter: DocumentAdapter
    private var service: CatalogService? = null
    private lateinit var progressBar: ProgressBar // Объявим прогресс бар

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document)


        progressBar = findViewById(R.id.progressBar)

        val recyclerView: RecyclerView = findViewById(R.id.documentRecycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val space = 15
        recyclerView.addItemDecoration(SpaceItemDecoration(space))

        service = CatalogService()

        adapter = DocumentAdapter(mutableListOf())
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
        val intent = Intent(this, ProductsDocumentActivity::class.java)
        intent.putExtra("title", title)
        intent.putExtra("idDocument", idDocument)
        startActivity(intent)
    }
}
