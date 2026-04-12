package com.animan.wholesalemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.animan.wholesalemanager.repository.CustomerRepository
import com.animan.wholesalemanager.data.local.Bill

class BillViewModel : ViewModel() {

    private val repository = CustomerRepository()

    var billList = mutableStateOf<List<Bill>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun fetchBills() {
        isLoading.value = true

        repository.getBills(
            onResult = {
                billList.value = it
                isLoading.value = false
            },
            onError = {
                errorMessage.value = it
                isLoading.value = false
            }
        )
    }
}