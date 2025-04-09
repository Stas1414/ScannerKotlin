package com.example.scannerkotlin.model

data class DocumentElement(
    var id: Int?,
    var amount: Int?,
    val docId: Long?,
    val elementId: Int?,
    val purchasingPrice: Double?,
    var storeFrom: Int?,
    var storeTo: Int?,
    var name: String?,
    var mainAmount: Double?
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