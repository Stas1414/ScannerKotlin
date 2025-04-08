package com.example.scannerkotlin.request

data class StoreAmountRequest(
    val select: MutableList<String> = mutableListOf("amount"),
    val filter: MutableMap<String, Any?>? = null
) {
}