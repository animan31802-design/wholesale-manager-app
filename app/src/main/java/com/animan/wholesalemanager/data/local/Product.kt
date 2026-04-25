package com.animan.wholesalemanager.data.local

data class Product(
    val id: String = "",
    val name: String = "",
    val sellingPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantity: Double = 0.0,        // ← Int → Double
    val unit: String = "Piece",
    val category: String = "",
    val minStockLevel: Double = 5.0,   // ← Int → Double
    val barcode: String = "",
    val gstPercent: Double = 0.0,
    val allowPartial: Boolean = false  // ← NEW
)