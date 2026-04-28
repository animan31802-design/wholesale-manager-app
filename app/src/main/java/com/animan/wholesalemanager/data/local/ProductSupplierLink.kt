package com.animan.wholesalemanager.data.local

import java.util.UUID

/**
 * Links a product to one or more suppliers.
 * Stored in the product_suppliers table.
 *
 * When the user taps a supplier link from the product edit screen,
 * the app navigates to PurchaseScreen with this product pre-selected.
 */
data class ProductSupplierLink(
    val id           : String = UUID.randomUUID().toString(),
    val productId    : String = "",
    val supplierId   : String = "",
    val supplierName : String = "",
    val costPrice    : Double = 0.0,   // last known cost from this supplier
    val note         : String = ""
)