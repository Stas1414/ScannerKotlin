package com.example.scannerkotlin.request

data class NewDocumentRequest(
    var fields: MutableMap<String, Any?>? = null
)