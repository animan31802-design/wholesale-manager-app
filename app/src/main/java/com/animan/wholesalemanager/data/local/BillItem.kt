package com.animan.wholesalemanager.data.local

data class BillItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,            // selling price at time of bill
    val costPrice: Double = 0.0,        // cost price at time of bill (for profit calc)
    val unit: String = "Piece",
    val quantity: Int = 1
)