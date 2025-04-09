package com.example.scannerkotlin.api

import com.example.scannerkotlin.request.*
import com.example.scannerkotlin.response.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiBitrix {

    @POST("catalog.document.list")
    suspend fun getDocumentListSuspend(@Body request: CatalogDocumentListRequest): CatalogDocumentListResponse

    @POST("catalog.document.element.list")
    fun getDocumentProducts(
        @Body list: CatalogDocumentElementListRequest
    ): Call<CatalogDocumentElementListResponse>

    @POST("catalog.product.offer.add")
    fun addVariationsOfProduct(
        @Body request: ProductOfferRequest
    ): Call<ProductOfferResponse>

    @POST("catalog.product.offer.list")
    fun getVariations(
        @Body request: VariationsRequest
    ): Call<VariationsResponse>

    @POST("catalog.product.list")
    fun getProducts (
        @Body request: ProductRequest
    ): Call<ProductResponse>


    @GET("barcode.getproductid.json")
    fun getProductIdByBarcode(
        @Query("barcode") barcode: String
    ): Call<ProductBarcodeResponse>

    @POST("barcode.setbyproductid.json")
    fun setBarcodeByProductId(
        @Body request: BarcodeRequest
    ): Call<HashMap<String, Any?>>

    @POST("catalog.product.get")
    fun getProductById(
        @Body productRequest: ProductIdRequest
    ) : Call<ProductResponse>

    @GET("catalog.document.conduct")
    fun conductDocument(
        @Query("id") id: Int
    ) : Call<HashMap<String, Any?>>

    @POST("catalog.document.element.update")
    fun updateDocumentElement(
        @Body request: UpdatedDocumentElementsRequest
    ) : Call<HashMap<String, Any?>>

    @POST("catalog.document.element.delete")
    fun deleteDocumentElement(
        @Body request: DeletedDocumentElementRequest
    ) : Call<HashMap<String, Any?>>

    @POST("catalog.product.update")
    fun updateProductMeasure(
        @Body request: UpdateProductMeasureRequest
    ) : Call<HashMap<String, Any?>>

    @POST("catalog.document.element.add")
    fun addDocumentElement(
        @Body request: AddDocumentElementRequest
    ) : Call<HashMap<String, Any?>>

    @GET("catalog.store.list")
    fun getStoreList():Call<HashMap<String, Any?>>

    @POST("catalog.document.add")
    fun addNewDocument(
        @Body request: NewDocumentRequest
    ) : Call<HashMap<String, Any?>>

    @POST("lists.element.get")
    fun getPasswords(@Body request: PasswordRequest): Call<PasswordResponse>

    @POST("catalog.storeproduct.list")
    fun getStoreAmount(
        @Body request:StoreAmountRequest
    ) : Call<StoreAmountResponse>
}