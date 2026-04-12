package com.animan.wholesalemanager.data.local

data class Bill(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val items: List<BillItem> = emptyList(),
    val itemsTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val balance: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)