package com.example.scannerkotlin.model

data class Product(
    val id: Int,
    val iblockId: Int,
    var name: String?,
    val measureId: Int?,
    var measureSymbol: String?,
    var quantity: Int?
)