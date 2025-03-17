package com.example.scannerkotlin.model

data class DocumentElement(
  val amount: Double?,
    val docId: Long?,
    val elementId: Long?,
    val purchasingPrice: Double?,
    val storeFrom: Int?,
    val storeTo: Int?
)