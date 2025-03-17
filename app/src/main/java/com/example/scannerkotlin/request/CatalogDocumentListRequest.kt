package com.example.scannerkotlin.request

data class CatalogDocumentListRequest(var order: List<String> = mutableListOf(),
                                      var filter: MutableMap<String, Any> = mutableMapOf(),
                                      var select: List<String> = mutableListOf(),
                                      var offset: Int? = null,
                                      var start: String? = null,
                                      var limit: Int? = null) {
}