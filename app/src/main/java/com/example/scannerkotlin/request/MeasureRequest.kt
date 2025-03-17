package com.example.scannerkotlin.request

data class MeasureRequest(
    val select: List<String> = mutableListOf("symbolIntl", "id"),
    val filter: Map<String, Any>? = null,
    val order: Map<String, String>? = null,
    val start: String? = null
)