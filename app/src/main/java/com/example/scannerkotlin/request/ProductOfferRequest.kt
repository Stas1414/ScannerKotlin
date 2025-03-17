package com.example.scannerkotlin.request

data class ProductOfferRequest(
    val select: List<String> = listOf("id", "iblockId", "name", "active", "available", "barcodeMulti", "parentId"),
    val filter: Map<String, Any> = mapOf("iblockId" to 15)
)