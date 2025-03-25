package com.example.scannerkotlin.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

data class ProductOffer(
    val product: Product
){
    val idInDocument: Int? get() = product.idInDocument
    var iblockId: Int? = null
    var catalogId: Int? = null
    val name: String? get() = product.name
    var active: String? = "Y"
    var code: String? = null
    var xmlId: String? = null
    var barcodeMulti: String? = "N"
    var canBuyZero: String? = "N"
    var createdBy: Int? = null
    var modifiedBy: Int? = null
    var dateActiveFrom: LocalDateTime? = null
    var dateActiveTo: LocalDateTime? = null
    @RequiresApi(Build.VERSION_CODES.O)
    val dateCreate: LocalDateTime = LocalDateTime.now()
    var iblockSectionId: Int? = null
    var iblockSection: List<Int>? = null
    val measure: Int? get() = product.measureId
    var previewText: String? = null
    var detailText: String? = null
    var previewPicture: Map<String, List<String>>? = null
    var detailPicture: Map<String, List<String>>? = null
    var previewTextType: String? = "text"
    var detailTextType: String? = "text"
    var sort: Int? = null
    var subscribe: String? = "D"
    var vatId: Int? = null
    var vatIncluded: String? = "N"
    var height: Double? = null
    var length: Double? = null
    var weight: Double? = null
    var width: Double? = null
    var quantityTrace: String? = "D"
    var purchasingCurrency: String? = "BYN"
    val purchasingPrice: Double = 0.0
    var quantity: Int? = product.quantity
    var quantityReserved: Double? = null
    var recurSchemeLength: Int? = 0
    var recurSchemeType: String? = "D"
    var trialPriceId: Int? = null
    var withoutOrder: String? = "N"
    val parentId: Int get() = product.id
    var properties: Map<String, Any>? = null
    val barcode: String? get() = product.barcode
}