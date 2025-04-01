package com.example.scannerkotlin.mappers

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.scannerkotlin.model.Store
import com.google.gson.internal.LinkedTreeMap
import java.text.SimpleDateFormat
import java.util.*

class StoreMapper {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

    fun mapToStore(storeMap: LinkedTreeMap<*, *>): Store {
        return Store(
            active = storeMap["active"] as? String ?: "",
            address = storeMap["address"] as? String ?: "",
            code = storeMap["code"] as? String,
            dateCreate = parseDate(storeMap["dateCreate"] as? String),
            dateModify = parseDate(storeMap["dateModify"] as? String),
            description = storeMap["description"] as? String ?: "",
            email = storeMap["email"] as? String,
            latitude = (storeMap["gpsN"] as? Double) ?: 0.0,
            longitude = (storeMap["gpsS"] as? Double) ?: 0.0,
            id = (storeMap["id"] as? Double)?.toInt() ?: 0,
            isIssuingCenter = storeMap["issuingCenter"] as? String ?: "N",
            modifiedBy = storeMap["modifiedBy"]?.toString(),
            phone = storeMap["phone"] as? String ?: "",
            schedule = storeMap["schedule"] as? String ?: "",
            sort = (storeMap["sort"] as? Double)?.toInt() ?: 100,
            title = storeMap["title"] as? String ?: "",
            userId = (storeMap["userId"] as? Double)?.toInt(),
            xmlId = storeMap["xmlId"] as? String
        )
    }

    private fun parseDate(dateString: String?): Date {
        return if (!dateString.isNullOrEmpty()) {
            try {
                dateFormat.parse(dateString) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        } else {
            Date()
        }
    }
}