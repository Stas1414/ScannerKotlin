package com.example.scannerkotlin.api

import com.example.scannerkotlin.request.AddDocumentElementRequest
import com.example.scannerkotlin.request.BarcodeRequest
import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.DeletedDocumentElementRequest
import com.example.scannerkotlin.request.NewDocumentRequest
import com.example.scannerkotlin.request.ProductIdRequest
import com.example.scannerkotlin.request.ProductOfferRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.request.UpdateProductMeasureRequest
import com.example.scannerkotlin.request.UpdatedDocumentElementsRequest
import com.example.scannerkotlin.request.VariationsRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ProductBarcodeResponse
import com.example.scannerkotlin.response.ProductOfferResponse
import com.example.scannerkotlin.response.ProductResponse
import com.example.scannerkotlin.response.UsersResponse
import com.example.scannerkotlin.response.VariationsResponse
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

    @GET("user.search")
    fun getUsers(): Call<UsersResponse>

}