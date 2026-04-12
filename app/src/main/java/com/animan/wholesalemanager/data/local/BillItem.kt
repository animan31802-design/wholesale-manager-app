package com.animan.wholesalemanager.data.local

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class BillItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val initialQuantity: Int = 1
) {
    var quantity by mutableStateOf(initialQuantity)
}