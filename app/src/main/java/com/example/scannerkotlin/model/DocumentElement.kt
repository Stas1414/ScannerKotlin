package com.example.scannerkotlin.model

data class DocumentElement(
    val id: Int?,
    var amount: Double?,
    val docId: Long?,
    val elementId: Int?,
    val purchasingPrice: Double?,
    var storeFrom: Int?,
    var storeTo: Int?,
    var name: String?
){
    fun checkInList(elements: List<DocumentElement>): Boolean {
        for (element in elements) {
            if (element == this) {
                return true
            }
        }
        return false
    }
}