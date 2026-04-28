package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.data.local.ProductSupplierLink
import com.animan.wholesalemanager.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductViewModel(app: Application) : AndroidViewModel(app) {

    var supplierLinks = mutableStateOf<List<ProductSupplierLink>>(emptyList())
    private val repository = ProductRepository(app.applicationContext)

    var productList  = mutableStateOf<List<Product>>(emptyList())
    val allProducts = mutableStateOf<List<Product>>(emptyList())
    var categoryList = mutableStateOf<List<String>>(emptyList())
    var frequentList = mutableStateOf<List<Product>>(emptyList())
    var lowStockList = mutableStateOf<List<Product>>(emptyList())
    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun fetchProducts() {
        isLoading.value = true
        repository.getProducts(
            onResult = { isLoading.value = false; allProducts.value = it; productList.value = it },
            onError  = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun fetchCategories()      { categoryList.value = repository.getDistinctCategories() }
    fun fetchFrequentProducts(){ frequentList.value = repository.getFrequentlySoldProducts(10) }
    fun fetchLowStockProducts(){ lowStockList.value = repository.getLowStockProducts() }

    fun searchProducts(query: String)     { productList.value = repository.searchProducts(query) }
    fun filterByCategory(category: String) {
        productList.value = if (category == "All") allProducts.value
        else allProducts.value.filter { it.category == category }
    }
    fun getProductByBarcode(barcode: String): Product? = repository.getProductByBarcode(barcode)

    fun addProduct(product: Product, onSuccess: () -> Unit) {
        if (product.name.isBlank()) { errorMessage.value = "Name cannot be empty"; return }
        if (product.sellingPrice < 0) { errorMessage.value = "Price cannot be negative"; return }
        isLoading.value = true
        repository.addProduct(product,
            onSuccess = { isLoading.value = false; fetchProducts(); fetchCategories(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.updateProduct(product,
            onSuccess = { isLoading.value = false; fetchProducts(); fetchCategories(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun deleteProduct(id: String, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.deleteProduct(id,
            onSuccess = { isLoading.value = false; fetchProducts(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun restockProduct(productId: String, qty: Double, onSuccess: () -> Unit) {
        if (qty == 0.0) return
        if (qty > 0) {
            repository.restockProduct(productId, qty,
                onSuccess = { fetchProducts(); onSuccess() },
                onError   = { errorMessage.value = it }
            )
        } else {
            // Negative qty = consumption / write-off
            repository.consumeStock(productId, -qty,
                onSuccess = { fetchProducts(); onSuccess() },
                onError   = { errorMessage.value = it }
            )
        }
    }

    fun fetchSupplierLinks(productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db    = com.animan.wholesalemanager.data.local.DatabaseHelper(getApplication())
            val links = db.getSuppliersByProduct(productId)
            withContext(Dispatchers.Main) { supplierLinks.value = links }
        }
    }

    fun addSupplierLink(link: ProductSupplierLink, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.animan.wholesalemanager.data.local.DatabaseHelper(getApplication())
            db.addProductSupplierLink(link)
            val updated = db.getSuppliersByProduct(link.productId)
            withContext(Dispatchers.Main) {
                supplierLinks.value = updated
                onSuccess()
            }
        }
    }

    fun removeSupplierLink(linkId: String, productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = com.animan.wholesalemanager.data.local.DatabaseHelper(getApplication())
            db.removeProductSupplierLink(linkId)
            val updated = db.getSuppliersByProduct(productId)
            withContext(Dispatchers.Main) { supplierLinks.value = updated }
        }
    }
}