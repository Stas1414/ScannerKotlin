package com.example.scannerkotlin.request

data class BarcodeRequest(
    var productid: Int,
    var barcode: String
)