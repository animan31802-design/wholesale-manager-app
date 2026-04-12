package com.animan.wholesalemanager.data.local

data class Bill(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val itemsTotal: Double = 0.0,
    val previousBalance: Double = 0.0,
    val finalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)