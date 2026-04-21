package com.animan.wholesalemanager.data.local

data class Customer(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val totalPurchase: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balance: Double = 0.0
)