package com.example.scannerkotlin.model

import java.util.Date

data class Store(

    val active: String,


    val address: String,


    val code: String?,


    val dateCreate: Date,

    val dateModify: Date,


    val description: String,


    val email: String?,


    val latitude: Double,


    val longitude: Double,


    val id: Int,


    val isIssuingCenter: String,


    val modifiedBy: String?,


    val phone: String,


    val schedule: String,


    val sort: Int,


    val title: String,


    val userId: Int?,


    val xmlId: String?
)


