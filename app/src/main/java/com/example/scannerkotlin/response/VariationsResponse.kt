package com.example.scannerkotlin.response

import com.google.gson.internal.LinkedTreeMap

data class VariationsResponse(
    val result: Result
) {
    data class Result(
        val offers: List<LinkedTreeMap<*, *>>
    )
}