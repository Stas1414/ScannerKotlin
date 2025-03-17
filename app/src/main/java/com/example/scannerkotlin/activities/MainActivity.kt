package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.scannerkotlin.R

class MainActivity : AppCompatActivity() {

    private var scanButton: Button? = null

    private var productsButton: Button? = null

    private var documentButton: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scanButton)
        productsButton = findViewById(R.id.productsButton)
        documentButton = findViewById(R.id.documentButton)

        scanButton?.setOnClickListener {
            startScanActivity()
        }

        productsButton?.setOnClickListener {
            startProductsActivity()
        }

        documentButton?.setOnClickListener {
            startDocumentActivity()
        }

    }

    private fun startScanActivity() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
    }

    private fun startProductsActivity() {
        val intent = Intent(this, ProductsActivity::class.java)
        startActivity(intent)
    }

    private fun startDocumentActivity() {
        val intent = Intent(this, DocumentActivity::class.java)
        startActivity(intent)
    }
}