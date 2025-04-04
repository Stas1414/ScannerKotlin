package com.example.scannerkotlin.mappers

import com.example.scannerkotlin.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserMapper {


    fun fromJson(json: Map<String, Any?>): User {
        return User(
            id = json["ID"] as? String ?: "",
            xmlId = json["XML_ID"] as? String ?: "",
            isActive = json["ACTIVE"] as? Boolean ?: false,
            name = json["NAME"] as? String ?: "",
            lastName = json["LAST_NAME"] as? String ?: "",
            secondName = json["SECOND_NAME"] as? String ?: "",
            title = json["TITLE"] as? String ?: "",
            email = json["EMAIL"] as? String ?: "",
            lastLogin = parseDate(json["LAST_LOGIN"] as? String),
            dateRegister = parseDate(json["DATE_REGISTER"] as? String),
            timeZone = json["TIME_ZONE"] as? String ?: "",
            isOnline = (json["IS_ONLINE"] as? String) == "Y",
            timeZoneOffset = json["TIME_ZONE_OFFSET"] as? String ?: "",
            timestampX = json["TIMESTAMP_X"] ?: null,
            lastActivityDate = json["LAST_ACTIVITY_DATE"] ?: null,
            personalGender = json["PERSONAL_GENDER"] as? String ?: "",
            personalProfession = json["PERSONAL_PROFESSION"] as? String ?: "",
            personalWww = json["PERSONAL_WWW"] as? String ?: "",
            personalBirthday = parseDate(json["PERSONAL_BIRTHDAY"] as? String),
            personalIcq = json["PERSONAL_ICQ"] as? String ?: "",
            personalPhone = json["PERSONAL_PHONE"] as? String ?: "",
            personalFax = json["PERSONAL_FAX"] as? String ?: "",
            personalMobile = json["PERSONAL_MOBILE"] as? String ?: "",
            personalPager = json["PERSONAL_PAGER"] as? String ?: "",
            personalStreet = json["PERSONAL_STREET"] as? String ?: "",
            personalCity = json["PERSONAL_CITY"] as? String ?: "",
            personalState = json["PERSONAL_STATE"] as? String ?: "",
            personalZip = json["PERSONAL_ZIP"] as? String ?: "",
            personalCountry = json["PERSONAL_COUNTRY"] as? String ?: "0",
            personalMailbox = json["PERSONAL_MAILBOX"] as? String ?: "",
            personalNotes = json["PERSONAL_NOTES"] as? String ?: "",
            workPhone = json["WORK_PHONE"] as? String ?: "",
            workCompany = json["WORK_COMPANY"] as? String ?: "",
            workPosition = json["WORK_POSITION"] as? String ?: "",
            workDepartment = json["WORK_DEPARTMENT"] as? String ?: "",
            workWww = json["WORK_WWW"] as? String ?: "",
            workFax = json["WORK_FAX"] as? String ?: "",
            workPager = json["WORK_PAGER"] as? String ?: "",
            workStreet = json["WORK_STREET"] as? String ?: "",
            workMailbox = json["WORK_MAILBOX"] as? String ?: "",
            workCity = json["WORK_CITY"] as? String ?: "",
            workState = json["WORK_STATE"] as? String ?: "",
            workZip = json["WORK_ZIP"] as? String ?: "",
            workCountry = json["WORK_COUNTRY"] as? String ?: "0",
            workProfile = json["WORK_PROFILE"] as? String ?: "",
            workNotes = json["WORK_NOTES"] as? String ?: "",
            ufEmploymentDate = json["UF_EMPLOYMENT_DATE"] as? String,
            ufDepartment = json["UF_DEPARTMENT"] as? List<Int> ?: emptyList(),
            ufPhoneInner = json["UF_PHONE_INNER"] as? String ?: "",
            userType = json["USER_TYPE"] as? String ?: ""
        )
    }

    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}