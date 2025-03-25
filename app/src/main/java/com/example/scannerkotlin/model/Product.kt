package com.example.scannerkotlin.model

data class Product(
    var idInDocument: Int?,
    val id: Int,
    val iblockId: Int,
    var name: String?,
    var measureId: Int?,
    var measureSymbol: String?,
    var quantity: Int?,
    var barcode: String?
){
    var measureName: String
        get() = when (measureId) {
            1 -> "Метр"
            2 -> "Литр"
            3 -> "Грамм"
            4 -> "Килограмм"
            5 -> "Штука"
            else -> "Unknown"
        }
        set(value) {
            measureId = when (value) {
                "Метр" -> 1
                "Литр" -> 2
                "Грамм" -> 3
                "Килограмм" -> 4
                "Штука" -> 5
                else -> 0
            }
        }

    constructor() : this(
        0,
        0,
        0,
        null,
        null,
        null,
        null,
        null,
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