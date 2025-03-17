package com.example.scannerkotlin.request

data class ProductRequest(
    val select: List<String> = mutableListOf("id", "iblockId", "name", "measure", "quantity"),
    val filter: Map<String, Any>? = null,
    val order: Map<String, String>? = null,
    val start: String? = null
)