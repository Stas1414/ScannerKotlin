package com.example.scannerkotlin.service

import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.UserMapper
import com.example.scannerkotlin.response.UserResponse
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
    private val userMapper = UserMapper()


    fun getUsers() {
        val callUser: Call<UserResponse>? = apiBitrix?.getUsers()
        callUser?.enqueue(object : Callback<UserResponse>{
            override fun onResponse(
                call: Call<UserResponse>,
                response: Response<UserResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()?.result ?: emptyList()
                    val users = result.map { userData ->
                        userMapper.fromJson(userData)
                    }
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }
}