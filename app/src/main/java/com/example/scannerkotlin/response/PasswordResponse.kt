package com.example.scannerkotlin.response

import com.google.gson.annotations.SerializedName

data class PasswordResponse(
    val result: List<ResultItem>
) {
    data class ResultItem(
        @field:SerializedName("PROPERTY_71")
        val propertyPass: Map<String, String>?,

        @field:SerializedName("PROPERTY_70")
        val propertyId: Map<String, String>?
    ) {

        fun getPassword(): String? = propertyPass?.values?.firstOrNull()
        fun getId(): String? = propertyId?.values?.firstOrNull()
    }
}