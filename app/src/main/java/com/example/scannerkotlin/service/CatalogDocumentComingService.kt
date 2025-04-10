package com.example.scannerkotlin.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.scannerkotlin.activities.MainActivity 
import com.example.scannerkotlin.api.ApiBitrix
import com.example.scannerkotlin.mappers.DocumentElementMapper
import com.example.scannerkotlin.mappers.DocumentMapper
import com.example.scannerkotlin.mappers.ProductMapper
import com.example.scannerkotlin.model.Document
import com.example.scannerkotlin.model.Product
import com.example.scannerkotlin.model.ProductOffer
import com.example.scannerkotlin.request.* 
import com.example.scannerkotlin.response.* 
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.* 
import retrofit2.HttpException
import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException 

@RequiresApi(Build.VERSION_CODES.O) 
class CatalogDocumentComingService {
    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"
    private val barcodeBaseUrl = "https://bitrix.izocom.by/rest/1/sh1lchx64vrzcor6/"

    private lateinit var apiBitrix: ApiBitrix
    private lateinit var apiBarcode: ApiBitrix
    private val gson = Gson()

    
    private val documentMapper = DocumentMapper()
    private val documentElementsMapper = DocumentElementMapper()
    private val productMapper = ProductMapper()

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiBitrix = retrofit.create(ApiBitrix::class.java)

        val barcodeRetrofit: Retrofit = Retrofit.Builder()
            .baseUrl(barcodeBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiBarcode = barcodeRetrofit.create(ApiBitrix::class.java)
    }

    

    
    private suspend fun conductDocSuspend(idDocument: Int, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Attempting to conduct document $idDocument")
                val response = apiBitrix.conductDocument(idDocument)
                if (response.isSuccessful) {
                    Log.d("ComingService", "Document $idDocument conducted successfully.")
                    true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ComingService", "Failed to conduct document $idDocument: ${response.code()} - $errorBody")
                    val errorDescription = try {
                        gson.fromJson(errorBody, ErrorResponse::class.java)?.errorDescription ?: "Unknown API error conducting document"
                    } catch (e: Exception) { "Failed to parse conduct error response" }

                    withContext(Dispatchers.Main) { 
                        showAlertInfo(errorDescription, context)
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e("ComingService", "Error conducting document $idDocument", e)
                withContext(Dispatchers.Main) {
                    showAlertInfo("Network or unexpected error conducting document.", context)
                }
                false
            }
        }
    }

    
    private suspend fun deleteProductsSuspend( 
        deletedProducts: List<Product>, 
        idDocument: Int,
        baseList: List<Product> 
    ): Boolean = coroutineScope {
        val productsToDelete = deletedProducts.filter { baseList.contains(it) && it.idInDocument != null }
        if (productsToDelete.isEmpty()) return@coroutineScope true

        Log.d("ComingService", "Deleting ${productsToDelete.size} products for document $idDocument")

        val deferredResults = productsToDelete.map { product ->
            async(Dispatchers.IO) { 
                try {
                    val request = DeletedDocumentElementRequest(
                        id = product.idInDocument!!, 
                        fields = mutableMapOf("docId" to idDocument)
                    )
                    val response = apiBitrix.deleteDocumentElement(request)
                    if (!response.isSuccessful) {
                        Log.e("ComingService", "Failed to delete product element ${product.idInDocument}: ${response.code()} - ${response.errorBody()?.string()}")
                        false
                    } else {
                        Log.d("ComingService", "Deleted product element ${product.idInDocument} successfully")
                        true
                    }
                } catch (e: Exception) {
                    Log.e("ComingService", "Error deleting product element ${product.idInDocument}", e)
                    false
                }
            }
        }
        deferredResults.awaitAll().all { it } 
    }

    
    private suspend fun updateProductsInDocSuspend( 
        updatedProducts: List<Product>, 
        idDocument: Int
    ): Boolean = coroutineScope {
        val productsToUpdate = updatedProducts.filter { it.idInDocument != null }
        if (productsToUpdate.isEmpty()) {
            Log.d("ComingService", "No products with idInDocument to update for document $idDocument")
            return@coroutineScope true
        }

        Log.d("ComingService", "Updating ${productsToUpdate.size} products for document $idDocument")

        val deferredResults = productsToUpdate.map { product ->
            async(Dispatchers.IO) { 
                var overallSuccess = false 
                try {
                    
                    val updateRequest = UpdatedDocumentElementsRequest(
                        id = product.idInDocument!!,
                        fields = mutableMapOf(
                            "docId" to idDocument,
                            "amount" to product.quantity.toString() 
                        )
                    )
                    Log.d("ComingService", "Updating doc element ${product.idInDocument} for product ${product.id}, amount ${product.quantity}")
                    val updateResponse = apiBitrix.updateDocumentElement(updateRequest)

                    if (updateResponse.isSuccessful) {
                        Log.d("ComingService", "Successfully updated doc element ${product.idInDocument}")

                        
                        val barcodeSuccess = if (!product.barcode.isNullOrBlank()) {
                            setBarcodeForProductSuspend(product.id, product.barcode!!)
                        } else {
                            Log.w("ComingService", "Skipping barcode set for product ${product.id} - barcode is null or blank")
                            true 
                        }

                        
                        
                        val measureUpdateSuccess = if (product.measureId != null) {
                            updateProductMeasureSuspend(product.id, product.measureId!!)
                        } else {
                            Log.w("ComingService", "Skipping measure update for product ${product.id} - measureId is null")
                            true 
                        }


                        overallSuccess = barcodeSuccess && measureUpdateSuccess
                        if (!overallSuccess) {
                            Log.e("ComingService", "Failed during barcode/measure update for product ${product.id} after successful element update.")
                        }

                    } else {
                        Log.e("ComingService", "Failed to update doc element ${product.idInDocument}: ${updateResponse.code()} - ${updateResponse.errorBody()?.string()}")
                        overallSuccess = false
                    }
                } catch (e: Exception) {
                    Log.e("ComingService", "Error updating product chain for ${product.id} (docElementId=${product.idInDocument})", e)
                    overallSuccess = false
                }
                overallSuccess 
            }
        }
        deferredResults.awaitAll().all { it } 
    }

    
    private suspend fun setBarcodeForProductSuspend(productId: Int, barcode: String): Boolean {
        val barcodeRequest = BarcodeRequest(productId, barcode)
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Setting barcode '$barcode' for product ID: $productId")
                val response = apiBarcode.setBarcodeByProductId(barcodeRequest)
                if (!response.isSuccessful) {
                    Log.e("ComingService", "Error setting barcode for $productId: ${response.code()} - ${response.errorBody()?.string()}")
                }
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("ComingService", "Network error setting barcode for $productId", e)
                false
            }
        }
    }

    
    private suspend fun updateProductMeasureSuspend(productId: Int, measureId: Int): Boolean {
        val request = UpdateProductMeasureRequest(
            id = productId,
            fields = mutableMapOf("measure" to measureId)
        )
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Updating measure to $measureId for product ID: $productId")
                val response = apiBitrix.updateProductMeasure(request)
                if (!response.isSuccessful) {
                    Log.e("ComingService", "Error updating measure for $productId: ${response.code()} - ${response.errorBody()?.string()}")
                }
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("ComingService", "Network error updating measure for $productId", e)
                false
            }
        }
    }

    
    suspend fun conductDocumentSuspend(
        baseList: List<Product>,
        idDocument: Int?,
        context: Context, 
        deletedProducts: List<Product>,
        updatedProducts: List<Product>,
        currentProductListState: List<Product>,
        productOffersList: List<ProductOffer>
    ): Boolean {
        if (idDocument == null) {
            Log.e("ComingService", "Error: Document ID is null for conduct.")
            return false
        }

        try {
            Log.d("ComingService", "Starting conduct process for document $idDocument")
            
            val deleteSuccess = deleteProductsSuspend(deletedProducts, idDocument, baseList)
            if (!deleteSuccess) {
                Log.e("ComingService", "Conduct failed at delete step for document $idDocument")
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при удалении товаров.", context) }
                return false
            }
            Log.d("ComingService", "Delete step successful.")

            
            val savedVariations = saveProductVariationsSuspend(productOffersList)
            Log.d("ComingService", "Save variations step resulted in ${savedVariations.size} saved products.")

            
            val addSuccess = if (savedVariations.isNotEmpty()) {
                addProductsToDocSuspend(savedVariations, idDocument, currentProductListState)
            } else { true }
            if (!addSuccess) {
                Log.e("ComingService", "Conduct failed at add step for document $idDocument")
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при добавлении новых товаров.", context) }
                return false
            }
            Log.d("ComingService", "Add step successful.")

            
            val updateSuccess = updateProductsInDocSuspend(updatedProducts, idDocument)
            if (!updateSuccess) {
                Log.e("ComingService", "Conduct failed at update step for document $idDocument")
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при обновлении товаров.", context) }
                return false
            }
            Log.d("ComingService", "Update step successful.")

            
            val conductSuccess = conductDocSuspend(idDocument, context) 

            return if (conductSuccess) {
                Log.d("ComingService", "Conduct final step successful for document $idDocument.")
                true 
            } else {
                Log.e("ComingService", "Conduct final step failed for document $idDocument")
                
                false 
            }

        } catch (e: Exception) {
            Log.e("ComingService", "Unexpected error during conduct process for doc $idDocument", e)
            
            withContext(Dispatchers.Main) { showAlertInfo("Непредвиденная ошибка при проведении: ${e.localizedMessage}", context) }
            return false
        }
        
    }



    
    private suspend fun addProductsToDocSuspend(
        savedVariations: List<Pair<Int, ProductOffer>>,
        idDocument: Int,
        
        currentProductListState: List<Product>
    ): Boolean = coroutineScope {
        if (savedVariations.isEmpty()) return@coroutineScope true

        Log.d("ComingService", "Adding ${savedVariations.size} doc elements for document $idDocument using current list state")

        val deferredResults = savedVariations.map { (elementId, productOffer) ->
            async(Dispatchers.IO) {
                var success = false
                try {
                    
                    
                    val currentProduct = currentProductListState.find { it.id == productOffer.product.id }

                    if (currentProduct == null) {
                        Log.e("ComingService", "CRITICAL: Cannot find product with ID ${productOffer.product.id} in currentProductListState during addProductsToDocSuspend!")
                        return@async false 
                    }

                    
                    val currentQuantity = currentProduct.quantity ?: 0 
                    Log.d("ComingService", "Using quantity $currentQuantity for product ${currentProduct.id} (Offer elementId: $elementId) from current list state.")

                    val request = AddDocumentElementRequest(
                        fields = mutableMapOf(
                            "docId" to idDocument,
                            "storeFrom" to 0,
                            "storeTo" to 1,
                            "elementId" to elementId, 
                            "amount" to currentQuantity.toString(), 
                            "purchasingPrice" to 0
                        )
                    )
                    val response = apiBitrix.addDocumentElement(request)
                    if (response.isSuccessful) {
                        val newDocElementId = try { (((response.body()
                            ?.get("result") as? Map<*, *>)
                            ?.get("documentElement") as? LinkedTreeMap<*, *>)
                            ?.get("id") as? Double)?.toInt() } catch (e: Exception) { null }

                        if (newDocElementId != null) {
                            
                            currentProduct.idInDocument = newDocElementId
                            Log.d("ComingService", "Added doc element for product ID $elementId, new docElementId: $newDocElementId. Updated idInDocument in current list state for product ${currentProduct.id}.")
                            success = true
                        } else {
                            Log.w("ComingService", "Added doc element for product ID $elementId, but failed to parse new docElementId")
                            success = true 
                        }
                    } else {
                        Log.e("ComingService", "Failed to add doc element for product ID $elementId: ${response.code()} - ${response.errorBody()?.string()}")
                        success = false
                    }
                } catch (e: Exception) {
                    Log.e("ComingService", "Error adding doc element for product ID ${productOffer.product.id}", e)
                    success = false
                }
                success
            }
        }
        deferredResults.awaitAll().all { it }
    }


    
    private suspend fun saveProductVariationsSuspend(
        productOffersList: List<ProductOffer> 
    ): List<Pair<Int, ProductOffer>> = coroutineScope { 
        if (productOffersList.isEmpty()) return@coroutineScope emptyList()

        Log.d("ComingService", "Saving ${productOffersList.size} product variations (offers)")

        val deferredResults = productOffersList.map { productOffer ->
            async(Dispatchers.IO) { 
                var savedPair: Pair<Int, ProductOffer>? = null
                try {
                    
                    val productId = addVariationOfProductSuspend(productOffer) 

                    if (productId != null) {
                        
                        val barcodeSuccess = if (!productOffer.barcode.isNullOrBlank()) {
                            setBarcodeForProductSuspend(productId, productOffer.barcode!!)
                        } else { true } 

                        if (barcodeSuccess) {
                            
                            savedPair = Pair(productId, productOffer)
                            Log.d("ComingService", "Successfully saved variation for ${productOffer.name} with ID $productId")
                        } else {
                            Log.e("ComingService", "Failed to set barcode for variation ${productOffer.name} (ID: $productId)")
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("ComingService", "Error saving variation for ${productOffer.name}", e)
                }
                savedPair 
            }
        }
        
        deferredResults.awaitAll().filterNotNull()
    }

    
    
    private suspend fun addVariationOfProductSuspend(productOffer: ProductOffer): Int? {
        
        val fieldsMap: MutableMap<String, Any?> = mutableMapOf(
            "iblockId" to 15, 
            "name" to productOffer.name,
            "measure" to productOffer.measure,
            "parentId" to productOffer.parentId,
            "dateCreate" to productOffer.dateCreate?.toString(), 
            "purchasingPrice" to productOffer.purchasingPrice,
            "purchasingCurrency" to productOffer.purchasingCurrency
        )

        
        val nonNullFields: MutableMap<String, Any> = fieldsMap
            .filterValues { it != null } 
            .mapValues { it.value!! } 
            .toMutableMap()           

        
        val productOfferRequest = ProductOfferRequest(fields = nonNullFields)

        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Adding variation for ${productOffer.name}")
                val response = apiBitrix.addVariationsOfProduct(productOfferRequest)
                if (response.isSuccessful) {
                    val productDetails = response.body()?.result?.get("offer") as? LinkedTreeMap<*, *>
                    val productId = productDetails?.get("id")?.let { (it as? Double)?.toInt() }
                    if (productId == null) {
                        Log.e("ComingService", "addVariationOfProduct successful but failed to parse offer ID from response")
                    }
                    productId 
                } else {
                    Log.e("ComingService", "Error adding variation ${productOffer.name}: ${response.code()} - ${response.errorBody()?.string()}")
                    null 
                }
            } catch (e: Exception) {
                Log.e("ComingService", "Network error adding variation ${productOffer.name}", e)
                null 
            }
        }
    }

    

    
    suspend fun getDocumentsSuspend(): List<Document> {
        val requestWithParams = CatalogDocumentListRequest(
            filter = mutableMapOf(
                "status" to "N",
                "docType" to "A" 
            )
        )
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Requesting documents list...")
                val response = apiBitrix.getDocumentListSuspend(requestWithParams) 

                if (response.isSuccessful) {
                    val rawDocuments = response.body()?.result?.get("documents") as? List<*>
                        ?: run {
                            Log.w("ComingService", "Documents list is null or not a list in response")
                            return@withContext emptyList<Document>()
                        }

                    rawDocuments.mapNotNull { documentMap ->
                        try {
                            documentMapper.mapToDocument(documentMap as LinkedTreeMap<*, *>)
                        } catch (e: Exception) {
                            Log.e("ComingService", "Error mapping document", e)
                            null
                        }
                    }.also { Log.d("ComingService", "Loaded ${it.size} documents") }
                } else {
                    Log.e("ComingService", "getDocumentsSuspend failed: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: HttpException) {
                Log.e("ComingService", "HTTP error in getDocumentsSuspend: ${e.code()} - ${e.message()}", e)
                emptyList()
            } catch (e: IOException) {
                Log.e("ComingService", "Network error in getDocumentsSuspend", e)
                emptyList()
            } catch (e: Exception) {
                Log.e("ComingService", "Failed to load documents", e)
                emptyList()
            }
        }
    }

    suspend fun getProductsForSelection(iblockId: Int): List<Product> {
        val productRequest = ProductRequest(filter = mapOf("iblockId" to iblockId))
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ComingService", "Getting products for selection, iblockId=$iblockId")
                val response = apiBitrix.getProducts(productRequest) 
                if (response.isSuccessful) {
                    val rawProducts = response.body()?.result?.get("products") as? List<*>
                    rawProducts?.mapNotNull { unit ->
                        (unit as? LinkedTreeMap<*, *>)?.let { productMap ->
                            try {
                                productMapper.mapToProduct(productMap)
                            } catch (e: Exception) {
                                Log.e("ComingService", "Error mapping product for selection", e)
                                null
                            }
                        }
                    } ?: emptyList<Product>().also {
                        Log.w("ComingService", "Product list for selection is null in response body.")
                    }
                } else {
                    Log.e("ComingService", "getProductsForSelection failed: ${response.code()} - ${response.message()}")
                    emptyList<Product>() 
                }
            } catch (e: Exception) {
                Log.e("ComingService", "Error in getProductsForSelection", e)
                emptyList<Product>() 
            }
        }
    }



    // Получение продуктов документа с деталями (suspend версия)
    // Получение продуктов документа с деталями (suspend версия)
    suspend fun getDocumentProductsWithDetails(idDocument: Int): List<Product> { // Возвращает List<Product>
        return withContext(Dispatchers.IO) {
            // Map<elementId, Pair<idInDocument, amount>>
            val documentElementsData = mutableMapOf<Int, Pair<Int, Int?>>() // amount может быть null

            try {
                // 1. Получаем элементы документа
                Log.d("ComingService", "Getting doc elements for document $idDocument")
                val requestElements = CatalogDocumentElementListRequest(filter = mutableMapOf("docId" to idDocument))
                val elementsResponse = apiBitrix.getDocumentProducts(requestElements) // suspend call

                if (elementsResponse.isSuccessful) {
                    val rawElements = elementsResponse.body()?.result?.get("documentElements") as? List<*>
                    rawElements?.forEach { element ->
                        (element as? LinkedTreeMap<*, *>)?.let { elementMap ->
                            try {
                                val docElement = documentElementsMapper.mapToDocumentElement(elementMap)
                                // Сохраняем ID элемента, ID записи в документе и КОЛИЧЕСТВО
                                if (docElement.elementId != null && docElement.id != null) {
                                    // ИСПРАВЛЕНО: Используем !! для id и elementId
                                    documentElementsData[docElement.elementId!!] = Pair(docElement.id!!, docElement.amount) // Сохраняем пару
                                    Log.d("ComingService", "Mapped doc element: id=${docElement.id}, elementId=${docElement.elementId}, amount=${docElement.amount}")
                                } else {
                                    Log.w("ComingService", "Skipping doc element due to null elementId or id: ${elementMap}")
                                }
                            } catch (mapE: Exception) {
                                Log.e("ComingService", "Error mapping document element", mapE)
                            }
                        }
                    }
                    Log.d("ComingService", "Found ${documentElementsData.size} elements data in document $idDocument")
                } else {
                    Log.w("ComingService", "getDocumentProducts failed: ${elementsResponse.code()} - ${elementsResponse.message()}")
                    return@withContext emptyList<Product>() // Критическая ошибка - выходим
                }

                // 2. Если элементы есть, получаем детали продуктов
                val elementIds = documentElementsData.keys.toList()
                if (elementIds.isEmpty()) {
                    Log.d("ComingService", "No valid element IDs found in document $idDocument, returning empty list.")
                    return@withContext emptyList<Product>()
                }

                Log.d("ComingService", "Getting product details for ${elementIds.size} elements")
                val productRequest = ProductRequest(filter = mapOf("id" to elementIds, "iblockId" to listOf(14, 15))) // Уточнить iblockId
                val productsResponse = apiBitrix.getProducts(productRequest) // suspend call
                val resultProducts = mutableListOf<Product>()

                if (productsResponse.isSuccessful) {
                    val rawProducts = productsResponse.body()?.result?.get("products") as? List<*>
                    rawProducts?.forEach { unit ->
                        (unit as? LinkedTreeMap<*, *>)?.let { productMap ->
                            try {
                                val product = productMapper.mapToProduct(productMap)
                                // Получаем сохраненные данные (ID в документе и количество)
                                val elementData = documentElementsData[product.id]

                                if (elementData != null) {
                                    product.idInDocument = elementData.first // Присваиваем ID из документа
                                    product.quantity = elementData.second // *** ПРИСВАИВАЕМ КОЛИЧЕСТВО ИЗ ДОКУМЕНТА ***
                                    Log.d("ComingService", "Assigned idInDocument=${product.idInDocument} and quantity=${product.quantity} to product ${product.id}")
                                } else {
                                    Log.w("ComingService", "Could not find element data for product ${product.id} in documentElementsData map.")
                                    product.idInDocument = null
                                    product.quantity = 0 // Или null, в зависимости от вашей модели
                                }
                                resultProducts.add(product)
                            } catch (mapE: Exception) {
                                Log.e("ComingService", "Error mapping product or assigning doc data", mapE)
                            }
                        }
                    }
                    Log.d("ComingService", "Mapped ${resultProducts.size} products with details and doc data.")
                } else {
                    Log.w("ComingService", "getProducts failed: ${productsResponse.code()} - ${productsResponse.message()}")
                    return@withContext emptyList<Product>() // Не смогли получить детали
                }
                resultProducts // Возвращаем список продуктов с idInDocument и quantity

            } catch (e: Exception) {
                Log.e("ComingService", "Error in getDocumentProductsWithDetails for doc $idDocument", e)
                emptyList()
            }
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
}