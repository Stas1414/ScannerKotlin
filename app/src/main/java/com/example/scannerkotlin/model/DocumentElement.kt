package com.example.scannerkotlin.model

data class DocumentElement(
    val id: Int?,
    val amount: Double?,
    val docId: Long?,
    val elementId: Int?,
    val purchasingPrice: Double?,
    val storeFrom: Int?,
    val storeTo: Int?
)