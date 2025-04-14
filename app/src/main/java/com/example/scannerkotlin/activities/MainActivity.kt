package com.example.scannerkotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.google.android.material.button.MaterialButton
import androidx.appcompat.app.AppCompatActivity

import com.example.scannerkotlin.R
import com.example.scannerkotlin.service.ScanService

import com.example.scannerkotlin.utils.SessionManager


class MainActivity : AppCompatActivity() {


    private lateinit var scanButton: MaterialButton
    private lateinit var productsButton: MaterialButton
    private lateinit var documentButton: MaterialButton
    private lateinit var logoutButton: MaterialButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (!SessionManager.isUserLoggedIn()) {
            Log.e("MainActivity", "User not logged in. Redirecting to AuthActivity.")

            goToAuthActivity()
            return
        }



        setContentView(R.layout.activity_main)


        try {
            val currentUserId = SessionManager.requireUserId()
            Log.d("MainActivity", "User logged in with ID: $currentUserId")

            startService(Intent(this, ScanService::class.java))
        } catch (e: IllegalStateException) {

            Log.e("MainActivity", "Error getting userId even after check.", e)
            Toast.makeText(this, "Ошибка сессии. Пожалуйста, перезайдите.", Toast.LENGTH_LONG).show()
            goToAuthActivity()
            return
        }



        scanButton = findViewById(R.id.scanButton)
        productsButton = findViewById(R.id.productsButton)
        documentButton = findViewById(R.id.documentButton)
        logoutButton = findViewById(R.id.logoutButton)


        scanButton.setOnClickListener {
            startScanActivity()
        }

        productsButton.setOnClickListener {
            startDocumentMovingActivity()
        }

        documentButton.setOnClickListener {
            startDocumentComingActivity()
        }

        logoutButton.setOnClickListener {
            performLogout()
        }
    }


    private fun startScanActivity() {
        val intent = Intent(this, ScanActivity::class.java)

        startActivity(intent)
    }


    private fun startDocumentMovingActivity() {
        val intent = Intent(this, DocumentMovingActivity::class.java)

        startActivity(intent)
    }


    private fun startDocumentComingActivity() {
        val intent = Intent(this, DocumentComingActivity::class.java)

        startActivity(intent)
    }


    private fun performLogout() {
        Log.d("MainActivity", "Logout button clicked.")

        SessionManager.clearSession()

        goToAuthActivity()
    }


    private fun goToAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java).apply {

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}