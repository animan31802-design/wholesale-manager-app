package com.animan.wholesalemanager.data.local

data class ConsumptionItem(
    val productId: String = "",
    val name: String = "",
    val costPrice: Double = 0.0,
    val unit: String = "Piece",
    val quantity: Int = 1
) {
    val totalCost: Double get() = costPrice * quantity
}