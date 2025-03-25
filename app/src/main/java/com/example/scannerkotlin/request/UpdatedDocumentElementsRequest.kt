package com.example.scannerkotlin.request

data class UpdatedDocumentElementsRequest(
    val id: Int?,
    val fields: MutableMap<String, Any?>? = null
) {
}