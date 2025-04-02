package com.example.scannerkotlin.request

data class VariationsRequest(
    val select: List<String> = listOf("id","iblockId", "name", "quantity"),
    val filter: Map<String, Int> = mapOf(
        "iblockId" to 15
    )
    ) {
}