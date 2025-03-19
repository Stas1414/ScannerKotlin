package com.example.scannerkotlin.api

import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.ProductIdRequest
import com.example.scannerkotlin.request.ProductOfferRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ProductBarcodeResponse
import com.example.scannerkotlin.response.ProductResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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


    @GET("barcode.getproductid.json")
    fun getProductIdByBarcode(
        @Query("barcode") barcode: String
    ): Call<ProductBarcodeResponse>

    @POST("catalog.product.get")
    fun getProductById(
        @Body productRequest: ProductIdRequest
    ) : Call<ProductResponse>

    @GET("catalog.document.conduct")
    fun conductDocument(
        @Query("id") id: Int
    ) : Call<Boolean>

}