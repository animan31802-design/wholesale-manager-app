package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.data.local.Product
import java.util.UUID

class ProductRepository(private val context: Context) {

    private val db by lazy { DatabaseHelper(context) }

    fun addProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newProduct = product.copy(id = UUID.randomUUID().toString())
        if (db.insertProduct(newProduct)) onSuccess()
        else onError("Failed to save product")
    }

    fun getProducts(onResult: (List<Product>) -> Unit, onError: (String) -> Unit) {
        try {
            onResult(db.getAllProducts())
        } catch (e: Exception) {
            onError(e.message ?: "Error loading products")
        }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.updateProduct(product)) onSuccess()
        else onError("Failed to update product")
    }

    fun deleteProduct(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteProduct(id)) onSuccess()
        else onError("Failed to delete product")
    }

    fun searchProducts(query: String): List<Product> = db.searchProducts(query)

    // Used when owner buys new stock — adds quantity to existing product
    fun restockProduct(
        productId: String,
        qty: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            db.incrementProductStock(productId, qty)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Restock failed")
        }
    }
}