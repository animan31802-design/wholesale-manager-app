package com.animan.wholesalemanager.data.local

import java.util.UUID

data class SupplierLedger(
    val id         : String = UUID.randomUUID().toString(),
    val supplierId : String = "",
    val amount     : Double = 0.0,
    val type       : String = "",   // "purchase" | "payment"
    val purchaseId : String = "",
    val note       : String = "",
    val timestamp  : Long   = System.currentTimeMillis()
)