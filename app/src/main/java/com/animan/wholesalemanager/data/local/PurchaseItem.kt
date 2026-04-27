package com.animan.wholesalemanager.data.local

data class PurchaseItem(
    val productId        : String = "",
    val name             : String = "",
    val costPrice        : Double = 0.0,   // price paid THIS purchase
    val previousCostPrice: Double = 0.0,   // old cost price before update
    val quantity         : Double = 1.0,
    val unit             : String = "Piece",
    val gstPercent       : Double = 0.0
) {
    val gstAmount  : Double get() = (costPrice * quantity * gstPercent) / 100.0
    val lineTotal  : Double get() = costPrice * quantity + gstAmount
}