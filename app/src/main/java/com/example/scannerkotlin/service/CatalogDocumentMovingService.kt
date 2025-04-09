package com.example.scannerkotlin.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.scannerkotlin.activities.DocumentMovingActivity
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.DocumentElementMapper
import com.example.scannerkotlin.mappers.DocumentMapper
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.mappers.StoreMapper
import com.example.scannerkotlin.model.Document
import com.example.scannerkotlin.model.DocumentElement
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.Store
import com.example.scannerkotlin.request.AddDocumentElementRequest
import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.DeletedDocumentElementRequest
import com.example.scannerkotlin.request.NewDocumentRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.request.StoreAmountRequest
import com.example.scannerkotlin.request.UpdatedDocumentElementsRequest
import com.example.scannerkotlin.request.VariationsRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ErrorResponse
import com.example.scannerkotlin.response.ProductResponse
import com.example.scannerkotlin.response.StoreAmountResponse
import com.example.scannerkotlin.response.VariationsResponse
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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


    fun getVariations(onComplete: (List<Product>) -> Unit) {

        val request = VariationsRequest()
        val callVariations: Call<VariationsResponse>? = apiBitrix?.getVariations(request)
        callVariations?.enqueue(object : Callback<VariationsResponse> {
            override fun onResponse(
                call: Call<VariationsResponse>,
                response: Response<VariationsResponse>
            ) {
                if (response.isSuccessful) {
                    val productMapper = ProductMapper()
                    val offers = response.body()?.result?.offers ?: emptyList()
                    val products = offers.map { map -> productMapper.mapToProduct(map) }
                    onComplete(products)
                } else {
                    onComplete(emptyList())
                }

            }

            override fun onFailure(call: Call<VariationsResponse>, t: Throwable) {
                Log.d("Error", "Error request")
                onComplete(emptyList())
            }

        })
    }

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

        val callDocumentList: Call<CatalogDocumentListResponse>? =
            apiBitrix?.getDocumentList(requestWithParams)
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
                                documentElements.add(
                                    documentElementsMapper.mapToDocumentElement(
                                        elementMap
                                    )
                                )
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
                                        Log.e(
                                            "CatalogService",
                                            "documentElement.id is null for product ${product.id}"
                                        )
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

    private suspend fun deleteProducts(
        deletedProducts: List<DocumentElement>,
        idDocument: Int,
        baseList: MutableList<DocumentElement>
    ): Boolean = suspendCoroutine { continuation ->
        if (deletedProducts.isEmpty()) {
            continuation.resume(true)
            return@suspendCoroutine
        }

        val latch = CountDownLatch(deletedProducts.size)
        var hasErrors = false

        deletedProducts.forEach { product ->
            if (product in baseList) {
                val request = DeletedDocumentElementRequest(
                    id = product.id,
                    fields = mutableMapOf("docId" to idDocument)
                )

                apiBitrix?.deleteDocumentElement(request)
                    ?.enqueue(object : Callback<HashMap<String, Any?>> {
                        override fun onResponse(
                            call: Call<HashMap<String, Any?>>,
                            response: Response<HashMap<String, Any?>>
                        ) {
                            if (!response.isSuccessful) {
                                hasErrors = true
                                Log.d("deletedElementError", "Error from deleting")
                            }
                            latch.countDown()
                        }

                        override fun onFailure(
                            call: Call<HashMap<String, Any?>>,
                            t: Throwable
                        ) {
                            hasErrors = true
                            Log.d("deletedElementError", "Network error: ${t.message}")
                            latch.countDown()
                        }
                    }) ?: run {
                    hasErrors = true
                    latch.countDown()

                }
            }

        }

        Thread {
            latch.await()
            continuation.resume(!hasErrors)
        }.start()
    }

    private suspend fun addProductsToDoc(
        newProducts: List<DocumentElement>,
        idDocument: Int,
        updatedProducts: MutableList<DocumentElement>
    ): Boolean = coroutineScope {
        val deferredResults = newProducts.map { element ->
            async {
                try {
                    val request = AddDocumentElementRequest(
                        fields = mutableMapOf(
                            "docId" to idDocument,
                            "storeFrom" to element.storeFrom,
                            "storeTo" to element.storeTo,
                            "elementId" to element.elementId,
                            "amount" to element.amount?.toString(),
                            "purchasingPrice" to element.purchasingPrice
                        )
                    )

                    val response = apiBitrix?.addDocumentElement(request)?.awaitResponse()
                    if (response?.isSuccessful == true) {
                        val responseBody = response.body() as? Map<*, *>
                        val result = responseBody?.get("result") as? Map<*, *>
                        val documentElement = result?.get("documentElement") as? Map<*, *>

                        val id = (documentElement?.get("id") as? Double)?.toInt()

                        if (id != null) {
                            updatedProducts.firstOrNull { it.elementId == element.elementId }?.id =
                                id
                            Log.d("DocumentUpdate", "Assigned ID in document: ${element.name}, $id")
                            true
                        } else {
                            Log.e(
                                "DocumentUpdate",
                                "Failed to parse ID for element: ${element.name}"
                            )
                            false
                        }
                    } else {
                        Log.e(
                            "DocumentUpdate",
                            "Failed to add element: ${element.name}, response: ${response?.code()}"
                        )
                        false
                    }
                } catch (e: Exception) {
                    Log.e("DocumentUpdate", "Error adding element ${element.name}", e)
                    false
                }
            }
        }


        deferredResults.awaitAll().all { it }
    }

    private suspend fun updateProductsInDoc(
        updatedProducts: List<DocumentElement>,
        idDocument: Int
    ): Boolean = suspendCoroutine { continuation ->
        Log.d("qqq", "updateProductsInDoc: started with ${updatedProducts.size} products for document $idDocument")

        if (updatedProducts.isEmpty()) {
            Log.d("qqqq", "updateProductsInDoc: empty products list - returning true")
            continuation.resume(true)
            return@suspendCoroutine
        }

        val latch = CountDownLatch(updatedProducts.size)
        var hasErrors = false

        for (product in updatedProducts) {
            Log.d("qqqq", "updateProductsInDoc: processing product ${product.elementId} (docElementId=${product.id})")

            val updateRequest = UpdatedDocumentElementsRequest(
                id = product.id,
                fields = mutableMapOf(
                    "docId" to idDocument,
                    "amount" to product.amount.toString()
                )
            )

            Log.d("qqqq", "updateProductsInDoc: sending update request for product ${product.id}: $updateRequest")

            apiBitrix?.updateDocumentElement(updateRequest)
                ?.enqueue(object : Callback<HashMap<String, Any?>> {
                    override fun onResponse(
                        call: Call<HashMap<String, Any?>>,
                        response: Response<HashMap<String, Any?>>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("qqqq", "updateProductsInDoc: successfully updated product ${product.id} in document")
                        } else {
                            Log.e("qqqq", "updateProductsInDoc: failed to update product ${product.id} in document. Response code: ${response.code()}, error: ${response.errorBody()?.string()}")
                            hasErrors = true
                        }
                        if (latch.count == 1L) { // Последний запрос завершился
                            continuation.resume(!hasErrors)
                        }
                        latch.countDown()
                        Log.d("qqqq", "updateProductsInDoc: remaining products to process: ${latch.count}")
                    }

                    override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                        Log.e("qqqq", "updateProductsInDoc: network error while updating product ${product.id}", t)
                        hasErrors = true
                        if (latch.count == 1L) { // Последний запрос завершился
                            continuation.resume(false)
                        }
                        latch.countDown()
                        Log.d("qqqq", "updateProductsInDoc: remaining products to process: ${latch.count}")
                    }
                }) ?: run {
                Log.e("qqqq", "updateProductsInDoc: apiBitrix is null or request creation failed for product ${product.id}")
                hasErrors = true
                if (latch.count == 1L) { // Последний запрос завершился
                    continuation.resume(false)
                }
                latch.countDown()
                Log.d("qqqq", "updateProductsInDoc: remaining products to process: ${latch.count}")
            }
        }
    }

    private fun conductDoc(
        idDocument: Int,
        context: Context,
        onLoading: (Boolean) -> Unit,
        callback: (Boolean) -> Unit
    ) {
        val callDocument = apiBitrix?.conductDocument(idDocument)
        callDocument?.enqueue(object : Callback<HashMap<String, Any?>> {
            override fun onResponse(
                call: Call<HashMap<String, Any?>>,
                response: Response<HashMap<String, Any?>>
            ) {
                onLoading(false)
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

            override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                onLoading(false)
                Log.d("conductError", t.stackTrace.toString())
                callback(false)
            }
        }) ?: run {
            onLoading(false)
            callback(false)
        }
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


    @RequiresApi(Build.VERSION_CODES.O)
    fun conductDocument(
        baseList: MutableList<DocumentElement>,
        idDocument: Int?,
        context: Context,
        deletedProducts: MutableList<DocumentElement>,
        updatedProducts: MutableList<DocumentElement>,
        newElement: MutableList<DocumentElement>,
        onLoading: (Boolean) -> Unit,
        callback: (Boolean) -> Unit
    ) {
        if (idDocument == null) {
            Log.e("conductDocument", "Ошибка: id документа отсутствует.")
            callback(false)
            return
        }

        onLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deleteSuccess = deleteProducts(deletedProducts, idDocument, baseList)
                if (!deleteSuccess) {
                    withContext(Dispatchers.Main) {
                        onLoading(false)
                        callback(false)
                    }
                    return@launch
                }


                val addSuccess = addProductsToDoc(newElement, idDocument, updatedProducts)
                if (!addSuccess) {
                    withContext(Dispatchers.Main) {
                        onLoading(false)
                        callback(false)
                    }
                    return@launch
                }


                val updateSuccess = updateProductsInDoc(updatedProducts, idDocument)
                if (!updateSuccess) {
                    withContext(Dispatchers.Main) {
                        onLoading(false)
                        callback(false)
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    conductDoc(idDocument, context, onLoading) { conductSuccess ->
                        if (conductSuccess) {
                            val intent = Intent(context, DocumentMovingActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        } else {
                            callback(false)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("conductDocument", "Ошибка: ${e.message}")
                withContext(Dispatchers.Main) {
                    onLoading(false)
                    callback(false)
                }
            }
        }
    }


    fun addNewDocument(userId: String?, callback: (Boolean) -> Unit) {
        if (userId == null) {
            callback(false)
            return
        }

        val request = NewDocumentRequest(fields = mutableMapOf(
            "docType" to "M",
            "currency" to "BYN",
            "responsibleId" to userId
        ))

        apiBitrix?.addNewDocument(request)?.enqueue(object : Callback<HashMap<String, Any?>> {
            override fun onResponse(call: Call<HashMap<String, Any?>>, response: Response<HashMap<String, Any?>>) {
                callback(response.isSuccessful)
            }

            override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                Log.d("Document Error", "Error: ${t.message}")
                callback(false)
            }
        })
    }

    fun getStoreAmount(storeId: Int?, productId: Int?, onAmount: (Double?) -> Unit?) {
        val request = StoreAmountRequest(
            filter = when {
                storeId != null && productId != null -> mutableMapOf(
                    "storeId" to storeId,
                    "productId" to productId
                )
                storeId != null -> mutableMapOf("storeId" to storeId)
                productId != null -> mutableMapOf("productId" to productId)
                else -> null
            }
        )

        val call = apiBitrix?.getStoreAmount(request)
        call?.enqueue(object : Callback<StoreAmountResponse> {
            override fun onResponse(
                call: Call<StoreAmountResponse>,
                response: Response<StoreAmountResponse>
            ) {
                if (response.isSuccessful) {
                    val storeProducts = response.body()?.result?.storeProducts
                    val amount = storeProducts?.firstOrNull()?.amount
                    onAmount(amount?.takeIf { it != 0.0 })
                } else {
                    onAmount(null)
                }
            }

            override fun onFailure(call: Call<StoreAmountResponse>, t: Throwable) {
                Log.d("StoreAmount", "StoreAmount Error", t)
                onAmount(null)
            }
        })
    }

    fun getStoreForScan(productId: Int, onComplete: (List<StoreAmountResponse.StoreProduct>?) -> Unit) {
        val request = StoreAmountRequest(
            select = mutableListOf("amount", "storeId"),
            filter = mutableMapOf(
                "productId" to productId
            )
        )
        apiBitrix?.getStoreAmount(request)?.enqueue(object : Callback<StoreAmountResponse> {
            override fun onResponse(
                call: Call<StoreAmountResponse>,
                response: Response<StoreAmountResponse>
            ) {
                if (response.isSuccessful) {
                    val storeProducts = response.body()?.result?.storeProducts
                    onComplete(storeProducts)
                }
            }

            override fun onFailure(call: Call<StoreAmountResponse>, t: Throwable) {
                onComplete(emptyList())
            }

        })
    }
}