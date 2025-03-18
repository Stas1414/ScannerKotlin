package com.example.scannerkotlin.model

data class Barcode(val data: String, val symbology: String) {



    override fun toString(): String {
        return "$data + \n$symbology"
    }

    fun checkInList(barcodes: MutableList<Barcode>): Boolean {
        for (barcode in barcodes) {
            if (barcode.data == data) {
                return true
            }
        }
        return false
    }
}