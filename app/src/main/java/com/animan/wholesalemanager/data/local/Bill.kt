package com.animan.wholesalemanager.data.local

data class Bill(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val items: List<BillItem> = emptyList(),
    val itemsTotal: Double = 0.0,      // sum of item totals before GST
    val gstTotal: Double = 0.0,        // sum of all GST amounts
    val grandTotal: Double = 0.0,      // itemsTotal + gstTotal
    val paidAmount: Double = 0.0,
    val balance: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isRefunded: Boolean = false,
    val refundedAt: Long = 0L
)