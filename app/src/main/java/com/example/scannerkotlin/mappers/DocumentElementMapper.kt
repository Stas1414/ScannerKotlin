package com.example.scannerkotlin.mappers

import com.example.scannerkotlin.model.DocumentElement
import com.google.gson.internal.LinkedTreeMap

class DocumentElementMapper {

    fun mapToDocumentElement(map: LinkedTreeMap<*, *>): DocumentElement {
        return DocumentElement(
            amount = (map["amount"] as? Double),
            docId = (map["docId"] as? Double)?.toLong(),
            elementId = (map["elementId"] as? Double)?.toInt(),
            purchasingPrice = (map["purchasingPrice"] as? Double),
            storeFrom = (map["storeFrom"] as? Double)?.toInt(),
            storeTo = (map["storeTo"] as? Double)?.toInt(),
            id = (map["id"] as? Double)?.toInt(),
            name = null
        )
    }
}