package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.data.local.Product
import java.util.UUID

class ProductRepository(private val context: Context) {

    private val db by lazy { DatabaseHelper(context) }

    fun addProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val new = product.copy(id = UUID.randomUUID().toString())
        if (db.insertProduct(new)) onSuccess() else onError("Failed to save product")
    }

    fun getProducts(onResult: (List<Product>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllProducts()) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun getProductsByCategory(cat: String): List<Product> = db.getProductsByCategory(cat)
    fun getDistinctCategories(): List<String> = db.getDistinctCategories()
    fun getFrequentlySoldProducts(limit: Int = 10): List<Product> = db.getFrequentlySoldProducts(limit)
    fun getLowStockProducts(): List<Product> = db.getLowStockProducts()
    fun searchProducts(query: String): List<Product> = db.searchProducts(query)
    fun getProductByBarcode(barcode: String): Product? = db.getProductByBarcode(barcode)

    fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.updateProduct(product)) onSuccess() else onError("Failed to update product")
    }

    fun deleteProduct(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteProduct(id)) onSuccess() else onError("Failed to delete product")
    }

    fun restockProduct(productId: String, qty: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try { db.incrementProductStock(productId, qty); onSuccess() }
        catch (e: Exception) { onError(e.message ?: "Restock failed") }
    }

    fun consumeStock(productId: String, qty: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try { db.decrementProductStock(productId, qty); onSuccess() }
        catch (e: Exception) { onError(e.message ?: "Consumption failed") }
    }
}