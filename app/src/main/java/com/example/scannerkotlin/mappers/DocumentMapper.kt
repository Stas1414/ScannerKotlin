package com.example.scannerkotlin.mappers

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.scannerkotlin.model.Document
import com.google.gson.internal.LinkedTreeMap
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DocumentMapper {
    @RequiresApi(Build.VERSION_CODES.O)
    fun mapToDocument(documentMap: LinkedTreeMap<*, *>): Document {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        return Document(
            commentary = documentMap["commentary"] as String?,
            createdBy = (documentMap["createdBy"] as Double).toInt(),
            currency = documentMap["currency"] as String,
            dateCreate = OffsetDateTime.parse(documentMap["dateCreate"] as String, formatter),
            dateDocument = OffsetDateTime.parse(documentMap["dateDocument"] as String, formatter),
            dateModify = OffsetDateTime.parse(documentMap["dateModify"] as String, formatter),
            dateStatus = OffsetDateTime.parse(documentMap["dateStatus"] as String, formatter),
            docNumber = documentMap["docNumber"] as String?,
            docType = documentMap["docType"] as String,
            id = (documentMap["id"] as Double).toInt(),
            modifiedBy = (documentMap["modifiedBy"] as Double).toInt(),
            responsibleId = (documentMap["responsibleId"] as Double).toInt(),
            siteId = documentMap["siteId"] as String,
            status = documentMap["status"] as String,
            statusBy = (documentMap["statusBy"] as Double?)?.toInt(),
            title = documentMap["title"] as String,
            total = documentMap["total"] as Double?
        )
    }
}