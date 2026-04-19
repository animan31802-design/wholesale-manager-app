package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.repository.ProductRepository

class ProductViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ProductRepository(app.applicationContext)

    var productList = mutableStateOf<List<Product>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun addProduct(name: String, price: Double, quantity: Int, category: String = "", onSuccess: () -> Unit) {
        if (name.isBlank()) { errorMessage.value = "Product name cannot be empty"; return }
        if (price < 0) { errorMessage.value = "Price cannot be negative"; return }
        if (quantity < 0) { errorMessage.value = "Quantity cannot be negative"; return }
        isLoading.value = true
        repository.addProduct(
            Product(name = name.trim(), price = price, quantity = quantity, category = category),
            onSuccess = { isLoading.value = false; fetchProducts(); onSuccess() },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun fetchProducts() {
        isLoading.value = true
        repository.getProducts(
            onResult = { isLoading.value = false; productList.value = it },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.updateProduct(product,
            onSuccess = { isLoading.value = false; fetchProducts(); onSuccess() },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun deleteProduct(id: String, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.deleteProduct(id,
            onSuccess = { isLoading.value = false; fetchProducts(); onSuccess() },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun restockProduct(productId: String, qty: Int, onSuccess: () -> Unit) {
        if (qty <= 0) { errorMessage.value = "Quantity must be greater than 0"; return }
        repository.restockProduct(productId, qty,
            onSuccess = { fetchProducts(); onSuccess() },
            onError = { errorMessage.value = it }
        )
    }

    fun searchProducts(query: String) {
        productList.value = repository.searchProducts(query)
    }
}