package com.animan.wholesalemanager.data.local

data class Ledger(
    val id: String = "",
    val customerId: String = "",
    val amount: Double = 0.0,
    val type: String = "", // CREDIT / PAYMENT
    val billId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)