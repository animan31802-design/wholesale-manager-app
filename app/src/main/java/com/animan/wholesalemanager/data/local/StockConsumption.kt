package com.animan.wholesalemanager.data.local

data class ConsumptionItem(
    val productId: String = "",
    val name: String = "",
    val costPrice: Double = 0.0,
    val unit: String = "Piece",
    val quantity: Double = 1.0
) {
    val totalCost: Double get() = costPrice * quantity
}