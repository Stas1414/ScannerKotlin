package com.example.scannerkotlin.mappers

import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Product

class ProductToDocumentElementMapper {

    fun map(
        product: Product,
        docId: Long? = null,
        storeFrom: Int? = null,
        storeTo: Int? = null
    ): DocumentElement {
        return DocumentElement(
            id = null,
            amount = null,
            docId = docId,
            elementId = product.id,
            purchasingPrice = 0.0,
            storeFrom = storeFrom,
            storeTo = storeTo,
            name = product.name,
            mainAmount = product.quantity?.toDouble()
        )
    }


    fun mapList(
        products: List<Product>,
        docId: Long? = null,
        storeFrom: Int? = null,
        storeTo: Int? = null
    ): List<DocumentElement> {
        return products.map { product ->
            map(product, docId, storeFrom, storeTo)
        }
    }
}