package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.repository.CustomerRepository

class CustomerViewModel : ViewModel() {

    private val repository = CustomerRepository()

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun addCustomer(name: String, phone: String, onSuccess: () -> Unit) {

        isLoading.value = true

        val customer = Customer(
            name = name,
            phone = phone
        )

        repository.addCustomer(
            customer,
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

    var customerList = mutableStateOf<List<Customer>>(emptyList())

    fun fetchCustomers() {
        isLoading.value = true

        repository.getCustomers(
            onResult = {
                isLoading.value = false
                customerList.value = it
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }

    fun createBill(
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        paidAmount: Double,
        onSuccess: () -> Unit
    ) {
        isLoading.value = true

        val previousBalance = customer.balance
        val finalAmount = itemsTotal + previousBalance
        val remaining = finalAmount - paidAmount

        println("ItemsTotal: $itemsTotal")
        println("Previous: $previousBalance")
        println("Final: $finalAmount")
        println("Paid: $paidAmount")
        println("Remaining: $remaining")

        val bill = Bill(
            customerId = customer.id,
            customerName = customer.name,
            items = items,
            itemsTotal = itemsTotal,
            paidAmount = paidAmount,
            balance = itemsTotal + customer.balance - paidAmount
        )

        repository.saveBill(
            bill,
            onSuccess = {
                updateCustomerBalance(customer.id, remaining)
                isLoading.value = false
                onSuccess()
            },
            onError = {
                isLoading.value = false
                errorMessage.value = it
            }
        )
    }
    fun updateCustomerBalance(customerId: String, newBalance: Double) {

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("customers")
            .document(customerId)
            .update("balance", newBalance)
    }
}