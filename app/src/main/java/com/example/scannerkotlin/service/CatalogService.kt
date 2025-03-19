package com.example.scannerkotlin.service

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.DocumentElementMapper
import com.example.scannerkotlin.mappers.DocumentMapper
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.model.Document
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ErrorResponse
import com.example.scannerkotlin.response.ProductResponse
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CatalogService {
    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiBitrix: ApiBitrix? = retrofit.create(ApiBitrix::class.java)

    private val documentMapper = DocumentMapper()
    private val documentElementsMapper = DocumentElementMapper()
    private val productMapper = ProductMapper()

    fun conductDocument(idDocument: Int?, context: Context, callback: (Boolean) -> Unit) {
        val callDocument = apiBitrix?.conductDocument(idDocument)
        callDocument?.enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    val errorDescription: ErrorResponse? = response.errorBody()?.string()?.let {
                        Gson().fromJson(it, ErrorResponse::class.java)
                    }
                    showAlertInfo(errorDescription?.errorDescription ?: "Unknown error", context)
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                showAlertInfo(t.message ?: "Unknown error", context)
                callback(false)
            }
        })
    }

    fun performDocumentListRequest(
        onComplete: (List<Document>) -> Unit,
        onError: (String) -> Unit
    ): MutableList<Document> {
        val documentsList = mutableListOf<Document>()
        val requestWithParams = CatalogDocumentListRequest(
            filter = mutableMapOf("status" to "N")
        )

        val callDocumentList: Call<CatalogDocumentListResponse>? =
            apiBitrix?.getDocumentList(requestWithParams)
        callDocumentList?.enqueue(object : Callback<CatalogDocumentListResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(
                call: Call<CatalogDocumentListResponse>,
                response: Response<CatalogDocumentListResponse>
            ) {
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
                val errorMessage = "Network failure: ${t.localizedMessage}"
                Log.e("NetworkError", errorMessage, t)
                onError(errorMessage)
            }
        })
        return documentsList
    }

    fun performDocumentElementsRequest(
        idDocument: Int,
        callback: (MutableList<Product>) -> Unit
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

                performFinalRequest(
                    idDocumentElements = idDocumentsElements,
                    onComplete = { products, _ ->
                        callback(products)
                    }
                )
            }

            override fun onFailure(call: Call<CatalogDocumentElementListResponse>, t: Throwable) {
                Log.e("CatalogService", "Network error (elements): ${t.message}", t)
                callback(mutableListOf())
            }
        })
    }

    private fun performFinalRequest(
        idDocumentElements: List<Long>,
        onComplete: (products: MutableList<Product>, measures: MutableMap<Int, String>) -> Unit
    ) {
        val productRequest = ProductRequest(
            filter = mapOf(
                "id" to idDocumentElements,
                "iblockId" to listOf(14, 15)
            )
        )
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

                onComplete(resultProducts, mutableMapOf())
            }

            override fun onFailure(call: Call<ProductResponse>, t: Throwable) {
                Log.e("performFinalRequest", "Network error: ${t.message}", t)
                onComplete(mutableListOf(), mutableMapOf())
            }
        })
    }

    private fun showAlertInfo(message: String, context: Context) {
            AlertDialog.Builder(context).apply {
                setTitle("Предупреждение")
                setMessage(message)
                setCancelable(false)
                setPositiveButton("Закрыть") { dialog, _ ->
                    dialog.cancel()
                }
            }.create().show()
    }
}