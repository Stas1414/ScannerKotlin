package com.example.scannerkotlin.api

import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.MeasureRequest
import com.example.scannerkotlin.request.ProductOfferRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.MeasureResponse
import com.example.scannerkotlin.response.ProductResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiBitrix {

    @POST("catalog.document.list")
    fun getDocumentList(
        @Body list: CatalogDocumentListRequest
    ) : Call<CatalogDocumentListResponse>

    @POST("catalog.document.element.list")
    fun getDocumentProducts(
        @Body list: CatalogDocumentElementListRequest
    ): Call<CatalogDocumentElementListResponse>

    @POST("catalog.product.offer.list")
    fun getVariationsOfProduct(
        @Body request: ProductOfferRequest
    ): Call<HashMap<String, Any>>

    @POST("catalog.product.list")
    fun getProducts (
        @Body request: ProductRequest
    ): Call<ProductResponse>

    @POST("catalog.measure.list")
    fun getMeasure(
        @Body measureRequest: MeasureRequest
    ) : Call<MeasureResponse>

}