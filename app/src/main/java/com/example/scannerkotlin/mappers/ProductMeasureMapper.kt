package com.example.scannerkotlin.mappers

import com.example.scannerkotlin.model.Product

data class ProductMeasureMapper(var products: MutableList<Product>) {


    constructor() : this(mutableListOf())

    fun setMeasureNameList() {
        for (product in products) {
            product.measureName = when(product.measureId) {
                1 -> "Метр"
                2 -> "Литр"
                3 -> "Грамм"
                4 -> "Килограмм"
                5 -> "Штука"
                else -> "Unknown"
            }
        }
    }

    fun setMeasureNameProduct(product: Product) : Product{
        product.measureName = when(product.measureId) {
            1 -> "Метр"
            2 -> "Литр"
            3 -> "Грамм"
            4 -> "Килограмм"
            5 -> "Штука"
            else -> "Unknown"
        }
        return product
    }
}