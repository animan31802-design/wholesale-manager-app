package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.local.Ledger
import com.animan.wholesalemanager.repository.CustomerRepository

class LedgerViewModel : ViewModel() {

    private val repository = CustomerRepository()

    var ledgerList = mutableStateOf<List<Ledger>>(emptyList())
    var isLoading = mutableStateOf(false)

    fun fetchLedger(customerId: String) {
        isLoading.value = true

        repository.getLedger(
            customerId,
            onResult = {
                ledgerList.value = it
                isLoading.value = false
            },
            onError = {
                isLoading.value = false
            }
        )
    }
}