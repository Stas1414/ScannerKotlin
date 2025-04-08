package com.example.scannerkotlin.response

import com.google.gson.annotations.SerializedName

data class StoreAmountResponse(
    val result: Result
) {
    data class Result(
        @field:SerializedName("storeProducts")
        val storeProducts: List<StoreProduct>
    )

    data class StoreProduct(
        @field:SerializedName("amount")
        val amount: Double
    )
}