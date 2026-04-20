package com.animan.wholesalemanager.data.local

data class BillItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,           // selling price at time of bill
    val costPrice: Double = 0.0,
    val unit: String = "Piece",
    val quantity: Int = 1,
    val gstPercent: Double = 0.0       // snapshot of GST at time of bill
) {
    val gstAmount: Double get() = (price * quantity) * (gstPercent / 100.0)
    val totalWithGst: Double get() = (price * quantity) + gstAmount
}