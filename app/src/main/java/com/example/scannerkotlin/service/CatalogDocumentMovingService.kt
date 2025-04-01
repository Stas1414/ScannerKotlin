package com.example.scannerkotlin.service

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.DocumentElementMapper
import com.example.scannerkotlin.mappers.DocumentMapper
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.mappers.StoreMapper
import com.example.scannerkotlin.model.Document
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.Store
import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ProductResponse
import com.google.gson.internal.LinkedTreeMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CatalogDocumentMovingService {

    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val apiBitrix: ApiBitrix? = retrofit.create(ApiBitrix::class.java)

    private val documentMapper = DocumentMapper()
    private val documentElementsMapper = DocumentElementMapper()
    private val productMapper = ProductMapper()




    fun getStoreList(
        onComplete: (List<Store>) -> Unit
    ) {
        val callStore: Call<HashMap<String, Any?>>? = apiBitrix?.getStoreList()
        callStore?.enqueue(object : Callback<HashMap<String, Any?>> {
            override fun onResponse(
                call: Call<HashMap<String, Any?>>,
                response: Response<HashMap<String, Any?>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    try {
                        val result = body?.get("result") as? Map<*, *>
                        val stores = result?.get("stores") as? List<*>

                        val storeMapper = StoreMapper()
                        val storeList = stores?.mapNotNull { store ->
                            (store as? LinkedTreeMap<*, *>)?.let { storeMapper.mapToStore(it) }
                        } ?: emptyList()

                        onComplete(storeList)
                    } catch (e: Exception) {
                        Log.e("API_ERROR", "Error parsing stores", e)
                        onComplete(emptyList())
                    }
                } else {
                    Log.e("API_ERROR", "Response not successful: ${response.code()}")
                    onComplete(emptyList())
                }
            }

            override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                Log.e("API_ERROR", "Failed to get store list", t)
                onComplete(emptyList())
            }
        })
    }

    fun performDocumentListRequest(
        onComplete: (List<Document>) -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        val documentsList = mutableListOf<Document>()
        val requestWithParams = CatalogDocumentListRequest(
            filter = mutableMapOf(
                "status" to "N",
                "docType" to "M"
            )
        )


        onLoading(true)

        val callDocumentList: Call<CatalogDocumentListResponse>? = apiBitrix?.getDocumentList(requestWithParams)
        callDocumentList?.enqueue(object : Callback<CatalogDocumentListResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<CatalogDocumentListResponse>,
                response: Response<CatalogDocumentListResponse>
            ) {

                onLoading(false)

                if (response.isSuccessful) {
                    val result = response.body() ?: run {
                        onError("Empty response body")
                        return
                    }

                    val rawDocuments = result.result["documents"] as? List<*>
                        ?: run {
                            onError("Invalid documents format")
                            return
                        }

                    documentsList.clear()
                    val mappedDocuments = rawDocuments.mapNotNull { documentMap ->
                        try {
                            documentMapper.mapToDocument(documentMap as LinkedTreeMap<*, *>)
                        } catch (e: Exception) {
                            Log.e("DocumentMapping", "Error mapping document: ${e.message}")
                            null
                        }
                    }

                    documentsList.addAll(mappedDocuments)
                    Log.d("DocumentLoad", "Loaded ${mappedDocuments.size} documents")
                    onComplete(mappedDocuments)

                } else {
                    val errorMessage = "HTTP error: ${response.code()} - ${response.message()}"
                    Log.e("NetworkError", errorMessage)
                    onError(errorMessage)
                }
            }

            override fun onFailure(call: Call<CatalogDocumentListResponse>, t: Throwable) {

                onLoading(false)

                val errorMessage = "Network failure: ${t.localizedMessage}"
                Log.e("NetworkError", errorMessage, t)
                onError(errorMessage)
            }
        })
    }

    fun performDocumentElementsRequest(
        idDocument: Int,
        callback: (MutableList<DocumentElement>) -> Unit
    ) {
        val documentElements = mutableListOf<DocumentElement>()

        val requestElementsWithParams = CatalogDocumentElementListRequest(
            filter = mutableMapOf("docId" to idDocument)
        )

        val callDocumentElementsList: Call<CatalogDocumentElementListResponse>? =
            apiBitrix?.getDocumentProducts(requestElementsWithParams)

        callDocumentElementsList?.enqueue(object : Callback<CatalogDocumentElementListResponse> {
            override fun onResponse(
                call: Call<CatalogDocumentElementListResponse>,
                response: Response<CatalogDocumentElementListResponse>
            ) {
                if (response.isSuccessful) {
                    val result: CatalogDocumentElementListResponse? = response.body()
                    result?.let {
                        val documentElement = it.result["documentElements"] as? ArrayList<*>
                        documentElement?.forEach { element ->
                            (element as? LinkedTreeMap<*, *>)?.let { elementMap ->
                                documentElements.add(documentElementsMapper.mapToDocumentElement(elementMap))
                            }
                        }
                    }
                }

                val idDocumentsElements = documentElements.mapNotNull { it.elementId }

                val productRequest = ProductRequest(
                    filter = mapOf(
                        "id" to idDocumentsElements,
                        "iblockId" to listOf(14, 15)
                    )
                )


                performFinalRequest(
                    productRequest,
                    onComplete = { products ->

                        for (product in products) {
                            for (documentElement in documentElements) {
                                if (product.id == documentElement.elementId) {
                                    if (documentElement.id != null) {
                                        documentElement.name = product.name
                                    } else {
                                        Log.e("CatalogService", "documentElement.id is null for product ${product.id}")
                                    }
                                }
                            }
                        }
                        callback(documentElements)
                    }
                )
            }

            override fun onFailure(call: Call<CatalogDocumentElementListResponse>, t: Throwable) {
                Log.e("CatalogService", "Network error (elements): ${t.message}", t)
                callback(mutableListOf())
            }
        })
    }


    fun performFinalRequest(
        productRequest: ProductRequest,
        onComplete: (products: MutableList<Product>) -> Unit
    ) {
        val callFinalRequest: Call<ProductResponse>? = apiBitrix?.getProducts(productRequest)
        callFinalRequest?.enqueue(object : Callback<ProductResponse> {
            override fun onResponse(
                call: Call<ProductResponse>,
                response: Response<ProductResponse>
            ) {
                val resultProducts: MutableList<Product> = mutableListOf()
                if (response.isSuccessful) {
                    val result: ProductResponse? = response.body()
                    if (result != null) {
                        val units: ArrayList<*>? = result.result["products"] as? ArrayList<*>
                        if (units != null && units.isNotEmpty()) {
                            for (unit: Any? in units) {
                                val product: LinkedTreeMap<*, *>? = unit as? LinkedTreeMap<*, *>
                                if (product != null) {
                                    resultProducts.add(productMapper.mapToProduct(product))
                                }
                            }
                        }
                    }
                } else {
                    Log.e("performFinalRequest", "Error code: ${response.code()}")
                }

                onComplete(resultProducts)
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                Log.e("performFinalRequest", "Network error: ${t.message}", t)
                onComplete(mutableListOf())
            }
        })
    }
}