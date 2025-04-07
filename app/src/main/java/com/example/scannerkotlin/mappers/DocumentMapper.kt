package com.example.scannerkotlin.mappers

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.scannerkotlin.model.Document
import com.google.gson.internal.LinkedTreeMap
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DocumentMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToDocument(documentMap: LinkedTreeMap<*, *>): Document? {
        return try {
            val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

            Document(
                commentary = documentMap["commentary"] as? String,
                createdBy = (documentMap["createdBy"] as? Double)?.toInt() ?: 0,
                currency = documentMap["currency"] as? String ?: "",
                dateCreate = parseDate(documentMap["dateCreate"] as? String, formatter),
                dateDocument = parseDate(documentMap["dateDocument"] as? String, formatter),
                dateModify = parseDate(documentMap["dateModify"] as? String, formatter),
                dateStatus = parseDate(documentMap["dateStatus"] as? String, formatter),
                docNumber = documentMap["docNumber"] as? String,
                docType = documentMap["docType"] as? String,
                id = (documentMap["id"] as? Double)?.toInt(),
                modifiedBy = (documentMap["modifiedBy"] as? Double)?.toInt(),
                responsibleId = (documentMap["responsibleId"] as? Double)?.toInt(),
                siteId = documentMap["siteId"] as? String,
                status = documentMap["status"] as? String,
                statusBy = (documentMap["statusBy"] as? Double)?.toInt(),
                title = documentMap["title"] as? String,
                total = documentMap["total"] as? Double
            )
        } catch (e: Exception) {
            Log.e("DocumentMapper", "Error mapping document: ${e.message}")
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDate(dateString: String?, formatter: DateTimeFormatter): OffsetDateTime? {
        return try {
            dateString?.let { OffsetDateTime.parse(it, formatter) }
        } catch (e: Exception) {
            Log.e("DocumentMapper", "Error parsing date: $dateString", e)
            null
        }
    }
}