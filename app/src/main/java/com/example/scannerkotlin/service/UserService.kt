package com.example.scannerkotlin.service

import android.util.Log
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.request.PasswordRequest
import com.example.scannerkotlin.response.PasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserService {

    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiBitrix: ApiBitrix? = retrofit.create(ApiBitrix::class.java)


    fun getPasswords(onComplete: (List<Pair<String, String>>?) -> Unit) {
        val request = PasswordRequest(iblockId = 18, iblockTypeId = "lists")
        val callUser = apiBitrix?.getPasswords(request)

        callUser?.enqueue(object : Callback<PasswordResponse> {
            override fun onResponse(
                call: Call<PasswordResponse>,
                response: Response<PasswordResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()?.result?.mapNotNull { item ->
                        val id = item.getId()
                        val password = item.getPassword()
                        if (id != null && password != null) {
                            id to password
                        } else {
                            null
                        }
                    }
                    onComplete(result)
                } else {
                    Log.d("Password Request", "Unsuccessful response: ${response.code()}")
                    onComplete(null)
                }
            }

            override fun onFailure(call: Call<PasswordResponse>, t: Throwable) {
                Log.d("Password Request", "Password request Error", t)
                onComplete(null)
            }
        })
    }
}