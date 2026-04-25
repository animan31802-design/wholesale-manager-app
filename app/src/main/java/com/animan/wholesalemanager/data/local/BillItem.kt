package com.animan.wholesalemanager.data.local

data class BillItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val unit: String = "Piece",
    val quantity: Double = 1.0,        // ← Int → Double
    val gstPercent: Double = 0.0
) {
    val gstAmount: Double get() = (price * quantity) * (gstPercent / 100.0)
    val totalWithGst: Double get() = (price * quantity) + gstAmount
}