package com.example.scannerkotlin.api

import com.example.scannerkotlin.request.*
import com.example.scannerkotlin.response.*
import retrofit2.Call // Убираем импорт Call
import retrofit2.Response // Используем Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiBitrix {

    @POST("catalog.document.list")
    suspend fun getDocumentListSuspend(@Body request: CatalogDocumentListRequest): Response<CatalogDocumentListResponse>

    @POST("catalog.document.element.list")
    suspend fun getDocumentProducts(
        @Body list: CatalogDocumentElementListRequest
    ): Response<CatalogDocumentElementListResponse>

    @POST("catalog.product.offer.add")
    suspend fun addVariationsOfProduct(
        @Body request: ProductOfferRequest
    ): Response<ProductOfferResponse>

    @POST("catalog.product.offer.list")
    suspend fun getVariations(
        @Body request: VariationsRequest
    ): Response<VariationsResponse>

    @POST("catalog.product.list")
    suspend fun getProducts (
        @Body request: ProductRequest
    ): Response<ProductResponse>


    @GET("barcode.getproductid.json")
    suspend fun getProductIdByBarcode(
        @Query("barcode") barcode: String
    ): Response<ProductBarcodeResponse>

    @POST("barcode.setbyproductid.json")
    suspend fun setBarcodeByProductId(
        @Body request: BarcodeRequest
    ): Response<HashMap<String, Any?>>


    @POST("catalog.product.get")
    suspend fun getProductById(
        @Body request: ProductIdRequest
    ): Response<ProductResponse>

    @GET("catalog.document.conduct")
    suspend fun conductDocument(
        @Query("id") id: Int
    ) : Response<HashMap<String, Any?>>

    @POST("catalog.document.element.update")
    suspend fun updateDocumentElement(
        @Body request: UpdatedDocumentElementsRequest
    ) : Response<HashMap<String, Any?>>

    @POST("catalog.document.element.delete")
    suspend fun deleteDocumentElement(
        @Body request: DeletedDocumentElementRequest
    ) : Response<HashMap<String, Any?>>

    @POST("catalog.product.update")
    suspend fun updateProductMeasure(
        @Body request: UpdateProductMeasureRequest
    ) : Response<HashMap<String, Any?>>

    @POST("catalog.document.element.add")
    suspend fun addDocumentElement(
        @Body request: AddDocumentElementRequest
    ) : Response<HashMap<String, Any?>>

    @GET("catalog.store.list")
    suspend fun getStoreList(): Response<HashMap<String, Any?>>

    @POST("catalog.document.add")
    suspend fun addNewDocument(
        @Body request: NewDocumentRequest
    ) : Response<HashMap<String, Any?>>

    @POST("lists.element.get")
    fun getPasswords(@Body request: PasswordRequest): Call<PasswordResponse>


    @POST("catalog.storeproduct.list")
    suspend fun getStoreAmount(
        @Body request: StoreAmountRequest
    ): Response<StoreAmountResponse>
}