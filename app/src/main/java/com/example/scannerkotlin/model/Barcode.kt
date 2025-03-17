package com.example.scannerkotlin.model

data class Barcode(val data: String, val symbology: String) {



    override fun toString(): String {
        return "$data + \n$symbology"
    }

    fun checkInList(products: MutableList<Barcode>): Boolean {
        for (product in products) {
            if (product.data == data) {
                return true
            }
        }
        return false
    }
}