package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animan.wholesalemanager.data.local.*
import com.animan.wholesalemanager.repository.SupplierRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SupplierViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SupplierRepository(app.applicationContext)

    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)
    var supplierList = mutableStateOf<List<Supplier>>(emptyList())
    var purchaseList = mutableStateOf<List<Purchase>>(emptyList())
    var ledgerList   = mutableStateOf<List<SupplierLedger>>(emptyList())

    // ── Suppliers ─────────────────────────────────────────────────────

    fun fetchSuppliers() {
        isLoading.value = true
        repository.getSuppliers(
            onResult = { isLoading.value = false; supplierList.value = it },
            onError  = { isLoading.value = false; errorMessage.value = it }
        )
    }

    // ── Fixed: accepts lat/lng ────────────────────────────────────────
    fun addSupplier(
        name     : String,
        phone    : String,
        address  : String,
        latitude : Double? = null,
        longitude: Double? = null,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) { errorMessage.value = "Name cannot be empty"; return }
        isLoading.value = true
        repository.addSupplier(
            Supplier(
                name      = name.trim(),
                phone     = phone.trim(),
                address   = address.trim(),
                latitude  = latitude,
                longitude = longitude
            ),
            onSuccess = { isLoading.value = false; fetchSuppliers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun updateSupplier(supplier: Supplier, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.updateSupplier(supplier,
            onSuccess = { isLoading.value = false; fetchSuppliers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun deleteSupplier(id: String, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.deleteSupplier(id,
            onSuccess = { isLoading.value = false; fetchSuppliers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun searchSuppliers(query: String) {
        supplierList.value = repository.searchSuppliers(query)
    }

    // ── Purchases ─────────────────────────────────────────────────────

    fun fetchAllPurchases() {
        isLoading.value = true
        repository.getAllPurchases(
            onResult = { isLoading.value = false; purchaseList.value = it },
            onError  = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun fetchPurchasesBySupplier(supplierId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getPurchasesBySupplier(supplierId)
            withContext(Dispatchers.Main) { purchaseList.value = list }
        }
    }

    fun fetchSupplierLedger(supplierId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getSupplierLedger(supplierId)
            withContext(Dispatchers.Main) { ledgerList.value = list }
        }
    }

    // ── Save purchase (suspend — used from coroutine in UI) ───────────

    suspend fun savePurchaseSuspend(purchase: Purchase, supplier: Supplier): String =
        suspendCancellableCoroutine { cont ->
            repository.savePurchase(purchase, supplier,
                onSuccess = { id -> if (cont.isActive) cont.resume(id) },
                onError   = { err -> if (cont.isActive) cont.resumeWithException(Exception(err)) }
            )
        }

    // ── Supplier payment ──────────────────────────────────────────────

    fun recordPayment(supplier: Supplier, amount: Double, note: String, onSuccess: () -> Unit) {
        if (amount <= 0)               { errorMessage.value = "Invalid amount"; return }
        if (amount > supplier.balance) { errorMessage.value = "Exceeds balance"; return }
        isLoading.value = true
        repository.recordSupplierPayment(supplier, amount, note,
            onSuccess = { isLoading.value = false; fetchSuppliers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it }
        )
    }

    // ── Mark purchase paid — updates balance in memory immediately ────
    fun markPurchasePaid(purchaseId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reducePurchaseBalance(purchaseId, amount)
            withContext(Dispatchers.Main) {
                purchaseList.value = purchaseList.value.map { p ->
                    if (p.id == purchaseId) {
                        p.copy(
                            paidAmount = p.paidAmount + amount,
                            balance    = (p.balance - amount).coerceAtLeast(0.0)
                        )
                    } else p
                }
            }
        }
    }

    fun generatePoNumber(): String = repository.generatePoNumber()
}