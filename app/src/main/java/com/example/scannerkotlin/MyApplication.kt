package com.example.scannerkotlin

import android.app.Application
import android.util.Log
import com.example.scannerkotlin.utils.SessionManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(applicationContext)
        Log.i("MyApplication", "SessionManager initialized.")
    }
}