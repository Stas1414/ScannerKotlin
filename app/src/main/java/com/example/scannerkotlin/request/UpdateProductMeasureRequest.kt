package com.example.scannerkotlin.request

data class UpdateProductMeasureRequest(
    var id: Int?,
    var fields: MutableMap<String, Any?>? = null
) {
}