package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scannerkotlin.R
import com.example.scannerkotlin.service.ScanService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private lateinit var scanButton: Button
    private lateinit var productsButton: Button
    private lateinit var documentButton: Button


    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        userId = intent.getStringExtra("userId")
        Log.d("MainActivity", "Received userId: $userId")


        startService(Intent(this, ScanService::class.java))


        scanButton = findViewById(R.id.scanButton)
        productsButton = findViewById(R.id.productsButton)
        documentButton = findViewById(R.id.documentButton)


        scanButton.setOnClickListener {
            lifecycleScope.launch {
                startScanActivity()
            }
        }

        productsButton.setOnClickListener {
            lifecycleScope.launch {
                startDocumentMovingActivity()
            }
        }

        documentButton.setOnClickListener {
            lifecycleScope.launch {
                startDocumentComingActivity()
            }
        }
    }


    private fun startScanActivity() {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
    }

    private fun startDocumentMovingActivity() {
        val intent = Intent(this, DocumentMovingActivity::class.java)
        intent.putExtra("userId", userId)
        Log.d("MainActivity", "Starting DocumentMovingActivity with userId: $userId")
        startActivity(intent)
    }

    private fun startDocumentComingActivity() {
        val intent = Intent(this, DocumentComingActivity::class.java)
        startActivity(intent)
    }
}