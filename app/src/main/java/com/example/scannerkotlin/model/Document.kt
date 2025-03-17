package com.example.scannerkotlin.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.internal.LinkedTreeMap
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class Document(
    val commentary: String?,
    val createdBy: Int,
    val currency: String,
    val dateCreate: OffsetDateTime,
    val dateDocument: OffsetDateTime,
    val dateModify: OffsetDateTime,
    val dateStatus: OffsetDateTime,
    val docNumber: String?,
    val docType: String,
    val id: Int,
    val modifiedBy: Int,
    val responsibleId: Int,
    val siteId: String,
    val status: String,
    val statusBy: Int?,
    val title: String,
    val total: Double?
)


