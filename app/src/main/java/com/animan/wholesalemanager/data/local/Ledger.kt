package com.animan.wholesalemanager.data.local

data class Ledger(
    val id: String = "",
    val customerId: String = "",
    val amount: Double = 0.0,
    val type: String = "",      // "CREDIT" or "PAYMENT"
    val billId: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)