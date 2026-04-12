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

    fun addProduct(name: String, price: Double, stock: Int, onSuccess: () -> Unit) {

        isLoading.value = true

        val product = Product(
            name = name,
            price = price,
            stock = stock
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
}