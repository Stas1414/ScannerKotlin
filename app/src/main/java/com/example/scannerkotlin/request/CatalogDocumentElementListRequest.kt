package com.example.scannerkotlin.request

data class CatalogDocumentElementListRequest(val order: List<String> = mutableListOf(),
                                             var filter: MutableMap<String, Any> = mutableMapOf(),
                                             val select: List<String> = mutableListOf(),
                                             val offset: Int? = null,
                                             val start: String? = null,
                                             val limit: Int? = null )