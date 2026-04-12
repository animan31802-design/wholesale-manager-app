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

    fun getTodaySummary(): Triple<Double, Double, Double> {

        val todayStart = getStartOfDay()

        val todayBills = billList.value.filter {
            it.timestamp >= todayStart
        }

        val total = todayBills.sumOf { it.itemsTotal }
        val paid = todayBills.sumOf { it.paidAmount }
        val balance = todayBills.sumOf { it.balance }

        return Triple(total, paid, balance)
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}