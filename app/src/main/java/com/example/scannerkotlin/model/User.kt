package com.example.scannerkotlin.model

import java.text.SimpleDateFormat
import java.util.*

data class User(
    val id: String,
    val xmlId: String,
    val isActive: Boolean,
    val name: String,
    val lastName: String,
    val secondName: String,
    val title: String,
    val email: String,
    val lastLogin: Date?,
    val dateRegister: Date?,
    val timeZone: String,
    val isOnline: Boolean,
    val timeZoneOffset: String,
    val timestampX: Any?,
    val lastActivityDate: Any?,
    val personalGender: String,
    val personalProfession: String,
    val personalWww: String,
    val personalBirthday: Date?,
    val personalIcq: String,
    val personalPhone: String,
    val personalFax: String,
    val personalMobile: String,
    val personalPager: String,
    val personalStreet: String,
    val personalCity: String,
    val personalState: String,
    val personalZip: String,
    val personalCountry: String,
    val personalMailbox: String,
    val personalNotes: String,
    val workPhone: String,
    val workCompany: String,
    val workPosition: String,
    val workDepartment: String,
    val workWww: String,
    val workFax: String,
    val workPager: String,
    val workStreet: String,
    val workMailbox: String,
    val workCity: String,
    val workState: String,
    val workZip: String,
    val workCountry: String,
    val workProfile: String,
    val workNotes: String,
    val ufEmploymentDate: String?,
    val ufDepartment: List<Int>,
    val ufPhoneInner: String,
    val userType: String
) {
}