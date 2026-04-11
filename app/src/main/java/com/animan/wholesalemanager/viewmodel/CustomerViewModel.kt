package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
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
}