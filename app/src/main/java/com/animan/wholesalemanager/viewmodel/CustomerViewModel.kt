package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.*
import com.animan.wholesalemanager.repository.CustomerRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CustomerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CustomerRepository(app.applicationContext)

    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)
    var customerList = mutableStateOf<List<Customer>>(emptyList())

    fun addCustomer(name: String, phone: String, address: String = "", latitude: Double? = null, longitude: Double? = null, onSuccess: () -> Unit) {
        if (name.isBlank()) { errorMessage.value = "Name cannot be empty"; return }
        isLoading.value = true
        repository.addCustomer(Customer(name = name.trim(), phone = phone.trim(), address = address.trim(), latitude = latitude, longitude=longitude),
            onSuccess = { isLoading.value = false; onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it })
    }

    fun fetchCustomers() {
        isLoading.value = true
        repository.getCustomers(
            onResult = { isLoading.value = false; customerList.value = it },
            onError  = { isLoading.value = false; errorMessage.value = it })
    }

    fun updateCustomer(customer: Customer, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.updateCustomer(customer,
            onSuccess = { isLoading.value = false; fetchCustomers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it })
    }

    fun deleteCustomer(id: String, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.deleteCustomer(id,
            onSuccess = { isLoading.value = false; fetchCustomers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it })
    }

    fun searchCustomers(query: String) {
        customerList.value = repository.searchCustomers(query)
    }

    // ── Callback version (unchanged — used elsewhere) ─────────────────

    fun createBill(
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        gstTotal: Double,
        grandTotal: Double,
        paidAmount: Double,
        onSuccess: (billId: String) -> Unit
    ) {
        if (items.isEmpty()) { errorMessage.value = "Add at least one item"; return }
        if (paidAmount < 0)  { errorMessage.value = "Paid amount cannot be negative"; return }
        isLoading.value = true

        val bill = buildBill(customer, items, itemsTotal, gstTotal, grandTotal, paidAmount)

        repository.saveBill(bill, customer,
            onSuccess = { billId ->
                isLoading.value = false
                fetchCustomers()
                onSuccess(billId)
            },
            onError = { isLoading.value = false; errorMessage.value = it })
    }

    // ── Suspend version — call from a coroutine scope on Dispatchers.IO ─
    // Returns the billId on success, throws on failure.

    suspend fun createBillSuspend(
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        gstTotal: Double,
        grandTotal: Double,
        paidAmount: Double
    ): String = suspendCancellableCoroutine { cont ->
        val bill = buildBill(customer, items, itemsTotal, gstTotal, grandTotal, paidAmount)
        repository.saveBill(bill, customer,
            onSuccess = { billId ->
                fetchCustomers()
                if (cont.isActive) cont.resume(billId)
            },
            onError = { err ->
                if (cont.isActive) cont.resumeWithException(Exception(err))
            }
        )
    }

    // ── Shared bill builder ───────────────────────────────────────────

    private fun buildBill(
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        gstTotal: Double,
        grandTotal: Double,
        paidAmount: Double
    ): Bill {
        val totalOwed = grandTotal + customer.balance
        val remaining = totalOwed - paidAmount
        return Bill(
            customerId   = customer.id,
            customerName = customer.name,
            items        = items,
            itemsTotal   = itemsTotal,
            gstTotal     = gstTotal,
            grandTotal   = grandTotal,
            paidAmount   = paidAmount,
            balance      = remaining,
            timestamp    = System.currentTimeMillis()
        )
    }

    fun refundBill(bill: Bill, customer: Customer, onSuccess: () -> Unit) {
        isLoading.value = true
        repository.refundBill(bill, customer,
            onSuccess = { isLoading.value = false; fetchCustomers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it })
    }

    fun recordPayment(customer: Customer, amount: Double, note: String, onSuccess: () -> Unit) {
        if (amount <= 0)               { errorMessage.value = "Invalid amount"; return }
        if (amount > customer.balance) { errorMessage.value = "Amount exceeds balance"; return }
        isLoading.value = true
        repository.recordPayment(customer, amount, note,
            onSuccess = { isLoading.value = false; fetchCustomers(); onSuccess() },
            onError   = { isLoading.value = false; errorMessage.value = it })
    }
}