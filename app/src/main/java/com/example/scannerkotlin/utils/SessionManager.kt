package com.example.scannerkotlin.utils

import android.content.Context
import android.content.SharedPreferences


object SessionManager {

    private const val PREFS_NAME = "AppSessionPrefs"
    private const val KEY_USER_ID = "userId"

    @Volatile
    private var sharedPreferences: SharedPreferences? = null


    @Volatile
    private var currentUserId: String? = null


    fun init(context: Context) {

        synchronized(this) {
            if (sharedPreferences == null) {
                sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                currentUserId = sharedPreferences?.getString(KEY_USER_ID, null)
            }
        }
    }


    fun saveSession(userId: String) {
        currentUserId = userId

        sharedPreferences?.edit()?.apply {
            putString(KEY_USER_ID, userId)
            apply()
        }
    }


    fun getUserId(): String? {
        if (currentUserId == null) {
            currentUserId = sharedPreferences?.getString(KEY_USER_ID, null)
        }
        return currentUserId
    }


    fun requireUserId(): String {
        return getUserId() ?: throw IllegalStateException("User ID is null. User must be logged in to call this method.")
    }


    fun isUserLoggedIn(): Boolean {
        return getUserId() != null
    }


    fun clearSession() {
        currentUserId = null
        sharedPreferences?.edit()?.apply {
            remove(KEY_USER_ID)
            apply()
        }
    }
}