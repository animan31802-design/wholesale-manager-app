package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.repository.ProductRepository

class ProductViewModel : ViewModel() {

    private val repository = ProductRepository()

    var productList = mutableStateOf<List<Product>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun addProduct(name: String, price: Double, quantity: Int, onSuccess: () -> Unit) {

        isLoading.value = true

        val product = Product(
            name = name,
            price = price,
            quantity = quantity
        )

        repository.addProduct(
            product,
            onSuccess = {
                isLoading.value = false
                onSuccess()
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }

    fun fetchProducts() {
        isLoading.value = true

        repository.getProducts(
            onResult = {
                isLoading.value = false
                productList.value = it
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }

    fun updateProduct(
        product: Product,
        onSuccess: () -> Unit
    ) {
        isLoading.value = true

        repository.updateProduct(
            product,
            onSuccess = {
                isLoading.value = false
                fetchProducts()
                onSuccess()
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }

    fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit
    ) {
        isLoading.value = true

        repository.deleteProduct(
            productId,
            onSuccess = {
                isLoading.value = false
                fetchProducts()
                onSuccess()
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }
}