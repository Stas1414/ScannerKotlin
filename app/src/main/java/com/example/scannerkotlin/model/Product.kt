package com.example.scannerkotlin.model

data class Product(
    val id: Int,
    val iblockId: Int,
    var name: String?,
    val measureId: Int?,
    var measureSymbol: String?,
    var measureName: String?,
    var quantity: Int?,
    var barcode: String?
){

    constructor() : this(
        0,
        0,
        null,
        null,
        null,
        null,
        null,
        null
    )

    fun checkInList(products: List<Product>): Boolean {
        for (product in products) {
            if (product == this) {
                return true
            }
        }
        return false
    }

    override fun toString(): String {
        return "Product(id=$id, iblockId=$iblockId, name=$name, measureId=$measureId, measureSymbol=$measureSymbol, measureName=$measureName, quantity=$quantity, barcode=$barcode)"
    }


}