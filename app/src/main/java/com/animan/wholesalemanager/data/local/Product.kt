package com.animan.wholesalemanager.data.local

data class Product(
    val id: String = "",
    val name: String = "",
    val sellingPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "Piece",          // Kg, Liter, Piece, Box, Packet, etc.
    val category: String = "",
    val minStockLevel: Int = 5,
    val barcode: String = "",             // optional, empty if not set
    val gstPercent: Double = 0.0      // 0 = no GST, 5 / 12 / 18 / 28 are common values
)