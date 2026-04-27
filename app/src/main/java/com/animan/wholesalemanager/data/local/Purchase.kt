package com.animan.wholesalemanager.data.local

import java.util.UUID

data class Purchase(
    val id           : String        = UUID.randomUUID().toString(),
    val supplierId   : String        = "",
    val supplierName : String        = "",
    val poNumber     : String        = "",   // auto-generated e.g. PO-20260424-001
    val items        : List<PurchaseItem> = emptyList(),
    val itemsTotal   : Double        = 0.0,
    val gstTotal     : Double        = 0.0,
    val grandTotal   : Double        = 0.0,
    val paidAmount   : Double        = 0.0,
    val balance      : Double        = 0.0,  // grandTotal - paidAmount
    val timestamp    : Long          = System.currentTimeMillis(),
    val note         : String        = ""
)