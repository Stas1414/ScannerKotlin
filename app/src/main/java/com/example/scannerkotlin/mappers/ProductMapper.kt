package com.example.scannerkotlin.mappers

import com.example.scannerkotlin.model.Product
import com.google.gson.internal.LinkedTreeMap

class ProductMapper {
    fun mapToProduct(map: LinkedTreeMap<*, *>): Product {
        return Product(
            idInDocument = null,
            id = (map["id"] as Double).toInt(),
            iblockId = (map["iblockId"] as Double).toInt(),
            name = map["name"] as String,
            measureId = (map["measure"] as? Double)?.toInt(),
            measureSymbol = null,
            quantity = (map["quantity"] as? Double)?.toInt(),
            barcode = null
        )
    }
}