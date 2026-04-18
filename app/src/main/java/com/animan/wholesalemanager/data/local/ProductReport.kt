package com.animan.wholesalemanager.data.local

data class ProductReport(
    val productId: String,
    val name: String,
    val totalQty: Int,
    val totalRevenue: Double
)