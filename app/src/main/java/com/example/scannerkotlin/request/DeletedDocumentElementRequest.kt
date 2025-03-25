package com.example.scannerkotlin.request

data class DeletedDocumentElementRequest(
    var id:Int?,
    var fields: MutableMap<String, Any?>? = null
)