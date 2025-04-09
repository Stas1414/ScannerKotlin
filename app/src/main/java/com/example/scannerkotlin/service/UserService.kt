package com.example.scannerkotlin.service

import android.util.Log
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.request.PasswordRequest
import com.example.scannerkotlin.response.PasswordResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserService {

    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiBitrix: ApiBitrix = retrofit.create(ApiBitrix::class.java)

    suspend fun getPasswords(): List<Pair<String, String>>? {
        return try {
            withContext(Dispatchers.IO) {
                val request = PasswordRequest(iblockId = 18, iblockTypeId = "lists")
                val response = apiBitrix.getPasswords(request).execute()
                processResponse(response)
            }
        } catch (e: HttpException) {
            Log.d("Password Request", "HTTP error: ${e.code()}", e)
            null
        } catch (e: Exception) {
            Log.d("Password Request", "Password request Error", e)
            null
        }
    }

    private fun processResponse(response: Response<PasswordResponse>): List<Pair<String, String>>? {
        return if (response.isSuccessful) {
            response.body()?.result?.mapNotNull { item ->
                val id = item.getId()
                val password = item.getPassword()
                if (id != null && password != null) {
                    id to password
                } else {
                    null
                }
            }
        } else {
            Log.d("Password Request", "Unsuccessful response: ${response.code()}")
            null
        }
    }
}