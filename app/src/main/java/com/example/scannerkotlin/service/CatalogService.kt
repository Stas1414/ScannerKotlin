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
import com.example.scannerkotlin.model.ProductOffer
import com.example.scannerkotlin.request.AddDocumentElementRequest
import com.example.scannerkotlin.request.BarcodeRequest
import com.example.scannerkotlin.request.CatalogDocumentElementListRequest
import com.example.scannerkotlin.request.CatalogDocumentListRequest
import com.example.scannerkotlin.request.DeletedDocumentElementRequest
import com.example.scannerkotlin.request.ProductOfferRequest
import com.example.scannerkotlin.request.ProductRequest
import com.example.scannerkotlin.request.UpdateProductMeasureRequest
import com.example.scannerkotlin.request.UpdatedDocumentElementsRequest
import com.example.scannerkotlin.response.CatalogDocumentElementListResponse
import com.example.scannerkotlin.response.CatalogDocumentListResponse
import com.example.scannerkotlin.response.ErrorResponse
import com.example.scannerkotlin.response.ProductOfferResponse
import com.example.scannerkotlin.response.ProductResponse
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.awaitResponse
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CatalogService {
    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"
    private val barcodeBaseUrl = "https://bitrix.izocom.by/rest/1/sh1lchx64vrzcor6/"

    private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private var barcodeRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(barcodeBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiBitrix: ApiBitrix? = retrofit.create(ApiBitrix::class.java)
    private val apiBarcode: ApiBitrix? = barcodeRetrofit.create(ApiBitrix::class.java)

    private val documentMapper = DocumentMapper()
    private val documentElementsMapper = DocumentElementMapper()
    private val productMapper = ProductMapper()




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

    private suspend fun deleteProducts(
        deletedProducts: List<Product>,
        idDocument: Int,
        baseList: MutableList<Product>
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
                    id = product.idInDocument,
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

    private suspend fun updateProductsInDoc(
        updatedProducts: List<Product>,
        idDocument: Int
    ): Boolean = suspendCoroutine { continuation ->
        Log.d("qqq","updateProductsInDoc: started with ${updatedProducts.size} products for document $idDocument ,  ${updatedProducts[0].name}, ${updatedProducts[1].name}")

        if (updatedProducts.isEmpty()) {
            Log.d("qqqq","updateProductsInDoc: empty products list - returning true")
            continuation.resume(true)
            return@suspendCoroutine
        }

        val latch = CountDownLatch(updatedProducts.size)
        var hasErrors = false

        for (product in updatedProducts) {
            Log.d("qqqq","updateProductsInDoc: processing product ${product.id} (docElementId=${product.idInDocument})")

            val updateRequest = UpdatedDocumentElementsRequest(
                id = product.idInDocument,
                fields = mutableMapOf(
                    "docId" to idDocument,
                    "amount" to product.quantity.toString()
                )
            )

            Log.d("qqqq","updateProductsInDoc: sending update request for product ${product.id}: $updateRequest")

            apiBitrix?.updateDocumentElement(updateRequest)?.enqueue(object : Callback<HashMap<String, Any?>> {
                override fun onResponse(call: Call<HashMap<String, Any?>>, response: Response<HashMap<String, Any?>>) {
                    if (response.isSuccessful) {
                        Log.d("qqqq","updateProductsInDoc: successfully updated product ${product.id} in document")

                        try {
                            Log.d("qqqq","updateProductsInDoc: setting barcode for product ${product.id}: ${product.barcode}")
                            setBarcodeForProduct(product.id, product.barcode.toString())

                            val updateMeasureRequest = UpdateProductMeasureRequest(
                                id = product.id,
                                fields = mutableMapOf("measure" to product.measureId)
                            )
                            Log.d("qqqq","updateProductsInDoc: updating measure for product ${product.id}: $updateMeasureRequest")
                            apiBitrix.updateProductMeasure(updateMeasureRequest)
                        } catch (e: Exception) {
                            Log.e("qqqq", "updateProductsInDoc: error while updating barcode/measure for product ${product.id}")
                            hasErrors = true
                        }
                    } else {
                        Log.e("qqqq","updateProductsInDoc: failed to update product ${product.id} in document. Response code: ${response.code()}, error: ${response.errorBody()?.string()}")
                        hasErrors = true
                    }
                    latch.countDown()
                    Log.d("qqqq","updateProductsInDoc: remaining products to process: ${latch.count}")
                }

                override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                    Log.e("qqqq", "updateProductsInDoc: network error while updating product ${product.id}")
                    hasErrors = true
                    latch.countDown()
                    Log.d("qqqq","updateProductsInDoc: remaining products to process: ${latch.count}")
                }
            }) ?: run {
                Log.e("qqqq","updateProductsInDoc: apiBitrix is null or request creation failed for product ${product.id}")
                hasErrors = true
                latch.countDown()
                Log.d("qqqq","updateProductsInDoc: remaining products to process: ${latch.count}")
            }
        }

        Thread {
            try {
                Log.d("qqqq","updateProductsInDoc: waiting for all products to be processed...")
                latch.await()
                Log.d("qqqq","updateProductsInDoc: all products processed. Final status: ${!hasErrors}")
                continuation.resume(!hasErrors)
            } catch (e: Exception) {
                Log.e("qqqq","updateProductsInDoc: error while waiting for latch")
                continuation.resume(false)
            }
        }.start()
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun conductDocument(
        baseList:MutableList<Product>,
        idDocument: Int?,
        context: Context,
        deletedProducts: MutableList<Product>,
        updatedProducts: MutableList<Product>,
        productOffersList: MutableList<ProductOffer>,
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
                // 1. Удаляем продукты из документа
                val deleteSuccess = deleteProducts(deletedProducts, idDocument, baseList)
                if (!deleteSuccess) {
                    withContext(Dispatchers.Main) {
                        onLoading(false)
                        callback(false)
                    }
                    return@launch
                }

                // 2. Сохраняем новые вариации продуктов (если есть)
                val savedProducts = saveProductVariations(productOffersList)// уже с баркодом и со всем

                // 3. Добавляем продукты в документ (если есть новые)
                if (savedProducts.isNotEmpty()) {
                    val addSuccess = addProductsToDoc(savedProducts, idDocument, updatedProducts)
                    if (!addSuccess) {
                        withContext(Dispatchers.Main) {
                            onLoading(false)
                            callback(false)
                        }
                        return@launch
                    }
                }

                // 4. Обновляем существующие продукты в документе
                val updateSuccess = updateProductsInDoc(updatedProducts, idDocument)
                if (!updateSuccess) {
                    withContext(Dispatchers.Main) {
                        onLoading(false)
                        callback(false)
                    }
                    return@launch
                }

                // 5. Проводим документ
                conductDoc(idDocument, context, onLoading, callback)

            } catch (e: Exception) {
                Log.e("conductDocument", "Ошибка: ${e.message}")
                withContext(Dispatchers.Main) {
                    onLoading(false)
                    callback(false)
                }
            }
        }
    }

    private suspend fun addProductsToDoc(
        savedProducts: List<Pair<Int, ProductOffer>>,
        idDocument: Int,
        updatedProducts: List<Product>
    ): Boolean = coroutineScope {
        val results = savedProducts.map { (elementId, productOffer) ->
            async {
                try {
                    val request = AddDocumentElementRequest(
                        fields = mutableMapOf(
                            "docId" to idDocument,
                            "storeFrom" to 0,
                            "storeTo" to 1,
                            "elementId" to elementId,
                            "amount" to productOffer.quantity.toString(),
                            "purchasingPrice" to 0
                        )
                    )

                    val response = apiBitrix?.addDocumentElement(request)?.awaitResponse()
                    if (response?.isSuccessful == true) {
                        val id: Int? = try {
                            (((response.body()
                                ?.get("result") as? Map<*, *>)
                                ?.get("documentElement") as? LinkedTreeMap<*, *>)
                            ?.get("id") as? Double)?.toInt()
                        } catch (e: Exception) {
                            null
                        }

                        updatedProducts.find { it == productOffer.product }?.idInDocument = id
                        Log.d("присвоение", "Присвоили ID в документе: ${productOffer.product.name}, $id")
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }

        results.awaitAll().all { it }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun saveProductVariations(
        productOffersList: List<ProductOffer>
    ): List<Pair<Int, ProductOffer>> = suspendCoroutine { continuation ->
        if (productOffersList.isEmpty()) {
            continuation.resume(emptyList())
            return@suspendCoroutine
        }

        val savedProducts = Collections.synchronizedList(mutableListOf<Pair<Int, ProductOffer>>())
        val latch = CountDownLatch(productOffersList.size)

        for (productOffer in productOffersList) {
            addVariationOfProduct(productOffer) { productId ->
                if (productId != null) {
                    setBarcodeForProduct(productId, productOffer.barcode.toString())
                    savedProducts.add(Pair(productId, productOffer))
                }
                latch.countDown()
            }
        }

        Thread {
            latch.await()
            continuation.resume(savedProducts)
        }.start()
    }



    private fun setBarcodeForProduct(productId: Int, barcode: String) {
        val barcodeRequest = BarcodeRequest(productId, barcode)
        val callBarcode: Call<HashMap<String, Any?>>? = apiBarcode?.setBarcodeByProductId(barcodeRequest)
        callBarcode?.enqueue(object : Callback<HashMap<String, Any?>> {
            override fun onResponse(call: Call<HashMap<String, Any?>>, response: Response<HashMap<String, Any?>>) {
                if (response.isSuccessful) {
                    Log.d("Barcode", "Barcode set successfully for product ID: $productId")
                } else {
                    Log.e("Barcode", "Error setting barcode: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<HashMap<String, Any?>>, t: Throwable) {
                Log.e("Barcode", "Network error: ${t.message}")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addVariationOfProduct(productOffer: ProductOffer, onComplete: (Int?) -> Unit) {
        val productOfferRequest = ProductOfferRequest(
            fields = mapOf(
                "iblockId" to 15,
                "name" to productOffer.name.toString(),
                "measure" to productOffer.measure.toString(),
                "parentId" to productOffer.parentId.toString(),
                "dateCreate" to productOffer.dateCreate.toString(),
                "purchasingPrice" to productOffer.purchasingPrice.toString(),
                "purchasingCurrency" to productOffer.purchasingCurrency.toString()
            )
        )

        val callOffer: Call<ProductOfferResponse>? = apiBitrix?.addVariationsOfProduct(productOfferRequest)
        callOffer?.enqueue(object : Callback<ProductOfferResponse> {
            override fun onResponse(call: Call<ProductOfferResponse>, response: Response<ProductOfferResponse>) {
                if (response.isSuccessful) {
                    val productDetails = response.body()?.result?.get("offer") as? LinkedTreeMap<*, *>
                    val productId = productDetails?.get("id")?.let { (it as? Double)?.toInt() }

                    if (productId != null) {
                        Log.d("Product", "Product ID: $productId")
                    } else {
                        Log.e("CatalogService", "Product details are null")
                    }
                    onComplete(productId)
                } else {
                    Log.e("API", "Ошибка: ${response.errorBody()?.string()}")
                    onComplete(null)
                }
            }

            override fun onFailure(call: Call<ProductOfferResponse>, t: Throwable) {
                Log.e("API", "Ошибка сети: ${t.message}")
                onComplete(null)
            }
        })
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun processDocumentUpdate(
//        deletedProducts: MutableList<Product>,
//        updatedProducts: MutableList<Product>,
//        productOffersList: MutableList<ProductOffer>,
//        idDocument: Int,
//        onLoading: (Boolean) -> Unit
//    ) {
//        onLoading(true)
//
//
//        deleteDocumentElement(deletedProducts, idDocument) { deleteSuccess ->
//            if (deleteSuccess) {
//                Log.d("processDocumentUpdate", "Удаление завершено. Начинаем сохранение вариаций продуктов.")
//
//                saveProductOffers(productOffersList) { savedProducts ->
//                    if (savedProducts.isNotEmpty()) {
//                        Log.d("processDocumentUpdate", "Вариации продуктов сохранены. Начинаем добавление в документ.")
//
//
//                        addProductsToDocument(savedProducts, idDocument) { addSuccess ->
//                            if (addSuccess) {
//                                Log.d("processDocumentUpdate", "Продукты добавлены в документ. Начинаем обновление.")
//
//
//                                updateProductInDocument(deletedProducts, updatedProducts, idDocument) {
//                                    Log.d("processDocumentUpdate", "Обновление завершено. Операция полностью завершена.")
//                                    onLoading(false)
//                                }
//                            } else {
//                                Log.d("processDocumentUpdate", "Ошибка при добавлении в документ.")
//                                onLoading(false)
//                            }
//                        }
//                    } else {
//                        Log.d("processDocumentUpdate", "Нет сохраненных продуктов. Переходим к обновлению.")
//                        updateProductInDocument(deletedProducts, updatedProducts, idDocument) {
//                            Log.d("processDocumentUpdate", "Обновление завершено. Операция полностью завершена.")
//                            onLoading(false)
//                        }
//                    }
//                }
//            } else {
//                Log.d("processDocumentUpdate", "Ошибка при удалении.")
//                onLoading(false)
//            }
//        }
//    }




    fun performDocumentListRequest(
        onComplete: (List<Document>) -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit
    ) {
        val documentsList = mutableListOf<Document>()
        val requestWithParams = CatalogDocumentListRequest(
            filter = mutableMapOf("status" to "N")
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
                                        product.idInDocument = documentElement.id
                                    } else {
                                        Log.e("CatalogService", "documentElement.id is null for product ${product.id}")
                                    }
                                }
                            }
                        }
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