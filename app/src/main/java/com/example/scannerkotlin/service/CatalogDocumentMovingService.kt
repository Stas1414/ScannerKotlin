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
import com.example.scannerkotlin.response.ErrorResponse
import com.example.scannerkotlin.response.StoreAmountResponse
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class CatalogDocumentMovingService {

    private val baseUrl = "https://bitrix.izocom.by/rest/1/o2deu7wx7zfl3ib4/"

    private var apiBitrix: ApiBitrix


    private val documentMapper = DocumentMapper()
    private val documentElementsMapper = DocumentElementMapper()
    private val productMapper = ProductMapper()
    private val storeMapper = StoreMapper()
    private val gson = Gson()


    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiBitrix = retrofit.create(ApiBitrix::class.java)
    }



    suspend fun getVariations(): List<Product> {
        val request = VariationsRequest()
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getVariations(request)
                if (response.isSuccessful) {
                    val offers = response.body()?.result?.offers ?: emptyList()
                    offers.mapNotNull { map ->
                        (map as? LinkedTreeMap<*, *>)?.let {
                            productMapper.mapToProduct(
                                it
                            )
                        }
                    }
                } else {
                    Log.w("ApiService", "getVariations failed: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error in getVariations", e)
                emptyList()
            }
        }
    }

    suspend fun getStoreList(): List<Store> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getStoreList()
                if (response.isSuccessful) {
                    val body = response.body()
                    val result = body?.get("result") as? Map<*, *>
                    val stores = result?.get("stores") as? List<*>

                    stores?.mapNotNull { store ->
                        (store as? LinkedTreeMap<*, *>)?.let { storeMapper.mapToStore(it) }
                    } ?: emptyList()
                } else {
                    Log.w("ApiService", "getStoreList failed: ${response.code()} - ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error in getStoreList", e)
                emptyList()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O) 
    suspend fun getDocumentsSuspend(): List<Document> {
        val requestWithParams = CatalogDocumentListRequest(
            filter = mutableMapOf(
                "status" to "N",
                "docType" to "M"
            )
        )
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getDocumentListSuspend(requestWithParams)
                if (response.isSuccessful) {
                    val rawDocuments = response.body()?.result?.get("documents") as? List<*>
                        ?: run {
                            Log.w("ApiService", "Documents list is null or not a list in response")
                            return@withContext emptyList<Document>()
                        }

                    rawDocuments.mapNotNull { documentMap ->
                        try {
                            documentMapper.mapToDocument(documentMap as LinkedTreeMap<*, *>)
                        } catch (e: Exception) {
                            Log.e("DocumentMapping", "Error mapping document: ${e.message}", e)
                            null
                        }
                    }.also { mappedDocuments ->
                        Log.d("DocumentLoad", "Loaded ${mappedDocuments.size} documents")
                    }
                } else {
                    Log.e("ApiService", "getDocumentsSuspend failed: ${response.code()} - ${response.message()}")
                    emptyList() 
                }

            } catch (e: HttpException) {
                Log.e("NetworkError", "HTTP error in getDocumentsSuspend: ${e.code()} - ${e.message()}", e)
                emptyList()
            } catch (e: IOException) {
                Log.e("NetworkError", "Network error in getDocumentsSuspend", e)
                emptyList()
            }
            catch (e: Exception) {
                Log.e("NetworkError", "Failed to load documents", e)
                emptyList()
            }
        }
    }


    
    suspend fun getDocumentElementsWithDetails(idDocument: Int): List<DocumentElement> {
        return withContext(Dispatchers.IO) {
            try {
                
                val requestElements = CatalogDocumentElementListRequest(filter = mutableMapOf("docId" to idDocument))
                val elementsResponse = apiBitrix.getDocumentProducts(requestElements)
                val documentElements = mutableListOf<DocumentElement>()

                if (elementsResponse.isSuccessful) {
                    val result = elementsResponse.body()?.result
                    val rawElements = result?.get("documentElements") as? List<*>
                    rawElements?.forEach { element ->
                        (element as? LinkedTreeMap<*, *>)?.let { elementMap ->
                            try {
                                documentElements.add(documentElementsMapper.mapToDocumentElement(elementMap))
                            } catch (mapE: Exception) {
                                Log.e("ApiService", "Error mapping document element", mapE)
                            }
                        }
                    }
                } else {
                    Log.w("ApiService", "getDocumentProducts failed: ${elementsResponse.code()} - ${elementsResponse.message()}")
                    
                    
                }

                
                if (documentElements.isNotEmpty()) {
                    val elementIds = documentElements.mapNotNull { it.elementId }
                    if (elementIds.isNotEmpty()) {
                        val productRequest = ProductRequest(filter = mapOf("id" to elementIds, "iblockId" to listOf(14, 15)))
                        val productsResponse = apiBitrix.getProducts(productRequest)
                        val productsMap = mutableMapOf<Int, Product>()

                        if (productsResponse.isSuccessful) {
                            val productResult = productsResponse.body()?.result
                            val rawProducts = productResult?.get("products") as? List<*>
                            rawProducts?.forEach { unit ->
                                (unit as? LinkedTreeMap<*, *>)?.let { productMap ->
                                    try {
                                        val product = productMapper.mapToProduct(productMap)
                                        productsMap[product.id] = product
                                    } catch (mapE: Exception) {
                                        Log.e("ApiService", "Error mapping product", mapE)
                                    }
                                }
                            }
                        } else {
                            Log.w("ApiService", "getProducts failed: ${productsResponse.code()} - ${productsResponse.message()}")
                        }

                        
                        documentElements.forEach { docElement ->
                            productsMap[docElement.elementId]?.let { product ->
                                docElement.name = product.name
                            }
                        }
                    }
                }
                documentElements 

            } catch (e: Exception) {
                Log.e("ApiService", "Error in getDocumentElementsWithDetails for doc $idDocument", e)
                emptyList() 
            }
        }
    }


    

    
    private suspend fun deleteProducts(
        deletedProducts: List<DocumentElement>,
        idDocument: Int,
        baseList: List<DocumentElement> 
    ): Boolean = coroutineScope { 
        val productsToDelete = deletedProducts.filter { it in baseList && it.id != null } 
        if (productsToDelete.isEmpty()) return@coroutineScope true

        Log.d("ApiService", "Deleting ${productsToDelete.size} products for document $idDocument")

        val deferredResults = productsToDelete.map { product ->
            async(Dispatchers.IO) { 
                try {
                    val request = DeletedDocumentElementRequest(
                        id = product.id!!, 
                        fields = mutableMapOf("docId" to idDocument)
                    )
                    val response = apiBitrix.deleteDocumentElement(request)
                    if (!response.isSuccessful) {
                        Log.e("ApiService", "Failed to delete product ${product.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        false
                    } else {
                        Log.d("ApiService", "Deleted product ${product.id} successfully")
                        true
                    }
                } catch (e: Exception) {
                    Log.e("ApiService", "Error deleting product ${product.id}", e)
                    false
                }
            }
        }
        deferredResults.awaitAll().all { it } 
    }


    
    private suspend fun addProductsToDoc(
        newProducts: List<DocumentElement>,
        idDocument: Int,
        
    ): Boolean = coroutineScope {
        if (newProducts.isEmpty()) return@coroutineScope true

        Log.d("ApiService", "Adding ${newProducts.size} products for document $idDocument")

        val deferredResults = newProducts.map { element ->
            async(Dispatchers.IO) {
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
                    val response = apiBitrix.addDocumentElement(request)
                    if (response.isSuccessful) {
                        
                        val responseBody = response.body()
                        val result = responseBody?.get("result") as? Map<*, *>
                        val documentElementMap = result?.get("documentElement") as? Map<*, *>
                        val id = (documentElementMap?.get("id") as? Double)?.toInt()
                        if (id != null) {
                            element.id = id 
                            Log.d("ApiService", "Added product ${element.name} with ID $id")
                            true
                        } else {
                            Log.w("ApiService", "Added product ${element.name} but failed to parse ID")
                            true 
                        }
                    } else {
                        Log.e("ApiService", "Failed to add element ${element.name}: ${response.code()} - ${response.errorBody()?.string()}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e("ApiService", "Error adding element ${element.name}", e)
                    false
                }
            }
        }
        deferredResults.awaitAll().all { it }
    }

    
    private suspend fun updateProductsInDoc(
        updatedProducts: List<DocumentElement>,
        idDocument: Int
    ): Boolean = coroutineScope {
        val productsToUpdate = updatedProducts.filter { it.id != null } 
        if (productsToUpdate.isEmpty()) {
            Log.d("ApiService", "updateProductsInDoc: No products with IDs to update for document $idDocument")
            return@coroutineScope true
        }

        Log.d("ApiService", "Updating ${productsToUpdate.size} products for document $idDocument")

        val deferredResults = productsToUpdate.map { product ->
            async(Dispatchers.IO) {
                try {
                    val updateRequest = UpdatedDocumentElementsRequest(
                        id = product.id!!, 
                        fields = mutableMapOf(
                            "docId" to idDocument,
                            "amount" to product.amount.toString() 
                        )
                    )
                    Log.d("ApiService", "Sending update for product ${product.id}: Amount ${product.amount}")
                    val response = apiBitrix.updateDocumentElement(updateRequest)
                    if (response.isSuccessful) {
                        Log.d("ApiService", "Successfully updated product ${product.id}")
                        true
                    } else {
                        Log.e("ApiService", "Failed to update product ${product.id}: ${response.code()} - ${response.errorBody()?.string()}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e("ApiService", "Error updating product ${product.id}", e)
                    false
                }
            }
        }
        deferredResults.awaitAll().all { it }
    }

    
    private suspend fun conductDocSuspend(idDocument: Int, context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ApiService", "Attempting to conduct document $idDocument")
                val response = apiBitrix.conductDocument(idDocument)
                if (response.isSuccessful) {
                    Log.d("ApiService", "Document $idDocument conducted successfully.")
                    true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ApiService", "Failed to conduct document $idDocument: ${response.code()} - $errorBody")
                    val errorDescription = try {
                        gson.fromJson(errorBody, ErrorResponse::class.java)?.errorDescription ?: "Unknown API error"
                    } catch (e: Exception) { "Failed to parse error response" }

                    
                    withContext(Dispatchers.Main) {
                        showAlertInfo(errorDescription, context)
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error conducting document $idDocument", e)
                withContext(Dispatchers.Main) {
                    showAlertInfo("Network or unexpected error conducting document.", context)
                }
                false
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


    
    @RequiresApi(Build.VERSION_CODES.O) 
    suspend fun conductDocumentSuspend( 
        baseList: List<DocumentElement>, 
        idDocument: Int?,
        context: Context,
        deletedProducts: List<DocumentElement>, 
        updatedProducts: List<DocumentElement>, 
        newElements: List<DocumentElement>      
        
    ): Boolean {
        if (idDocument == null) {
            Log.e("conductDocument", "Error: Document ID is null.")
            return false 
        }

        

        try {
            Log.d("ApiService", "Starting conduct process for document $idDocument")
            
            val deleteSuccess = deleteProducts(deletedProducts, idDocument, baseList)
            if (!deleteSuccess) {
                Log.e("ApiService", "Conduct failed at delete step for document $idDocument")
                
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при удалении товаров.", context) }
                return false
            }
            Log.d("ApiService", "Delete step successful for document $idDocument")

            
            val mutableNewElements = newElements.toMutableList() 
            val addSuccess = addProductsToDoc(mutableNewElements, idDocument) 
            if (!addSuccess) {
                Log.e("ApiService", "Conduct failed at add step for document $idDocument")
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при добавлении новых товаров.", context) }
                return false
            }
            Log.d("ApiService", "Add step successful for document $idDocument")

            
            val allProductsToPotentiallyUpdate = updatedProducts + mutableNewElements.filter { it.id != null }
            
            val updateSuccess = updateProductsInDoc(allProductsToPotentiallyUpdate, idDocument)
            if (!updateSuccess) {
                Log.e("ApiService", "Conduct failed at update step for document $idDocument")
                withContext(Dispatchers.Main) { showAlertInfo("Ошибка при обновлении товаров.", context) }
                return false
            }
            Log.d("ApiService", "Update step successful for document $idDocument")

            
            val conductSuccess = conductDocSuspend(idDocument, context)
            if (conductSuccess) {
                Log.d("ApiService", "Conduct final step successful for document $idDocument. Navigating...")
                
                withContext(Dispatchers.Main) {
                    val intent = Intent(context, MainActivity::class.java) 
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
                return true 
            } else {
                Log.e("ApiService", "Conduct final step failed for document $idDocument")
                
                return false
            }

        } catch (e: Exception) {
            Log.e("conductDocumentSuspend", "Unexpected error during conduct process for doc $idDocument", e)
            withContext(Dispatchers.Main) { showAlertInfo("Непредвиденная ошибка при проведении.", context) }
            return false
        } finally {
            
        }
    }


    
    suspend fun addNewDocument(userId: String?): Boolean { 
        if (userId.isNullOrBlank()) {
            Log.w("ApiService", "Cannot add new document: userId is null or blank.")
            return false
        }

        val request = NewDocumentRequest(fields = mutableMapOf(
            "docType" to "M",
            "currency" to "BYN",
            "responsibleId" to userId
        ))

        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.addNewDocument(request)
                if (!response.isSuccessful) {
                    Log.e("ApiService", "addNewDocument failed: ${response.code()} - ${response.errorBody()?.string()}")
                }
                response.isSuccessful 
            } catch (e: Exception) {
                Log.e("ApiService", "Error in addNewDocument", e)
                false 
            }
        }
    }


    suspend fun getStoreAmount(storeId: Int?, productId: Int?): Double? {

        val request = StoreAmountRequest(filter = mutableMapOf(
            "storeId" to storeId,
            "productId" to productId
        ))
        if (request.filter?.isEmpty() == true) {
            Log.w("ApiService", "getStoreAmount called with no storeId or productId.")
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getStoreAmount(request)
                if (response.isSuccessful) {
                    val storeProducts = response.body()?.result?.storeProducts
                    val amount = storeProducts?.firstOrNull()?.amount
                    amount?.takeIf { it != 0.0 } 
                } else {
                    Log.w("ApiService", "getStoreAmount failed: ${response.code()} - ${response.message()}")
                    null 
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error in getStoreAmount", e)
                null 
            }
        }
    }


    
    suspend fun getStoreForScan(productId: Int): List<StoreAmountResponse.StoreProduct> { 
        val request = StoreAmountRequest(
            select = mutableListOf("amount", "storeId"),
            filter = mutableMapOf("productId" to productId)
        )
        return withContext(Dispatchers.IO) {
            try {
                val response = apiBitrix.getStoreAmount(request)
                if (response.isSuccessful) {
                    response.body()?.result?.storeProducts ?: emptyList()
                } else {
                    Log.w("ApiService", "getStoreForScan failed for product $productId: ${response.code()} - ${response.message()}")
                    emptyList() 
                }
            } catch (e: Exception) {
                Log.e("ApiService", "Error in getStoreForScan for product $productId", e)
                emptyList() 
            }
        }
    }
}