package com.example.scannerkotlin.response


data class ErrorResponse(
    val error: Int,
    val errorDescription: String
)
