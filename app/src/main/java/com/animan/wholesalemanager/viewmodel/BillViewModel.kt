package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.repository.CustomerRepository
import java.util.Calendar

class BillViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CustomerRepository(app.applicationContext)

    var billList = mutableStateOf<List<Bill>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun fetchBills() {
        isLoading.value = true
        repository.getBills(
            onResult = { billList.value = it; isLoading.value = false },
            onError = { errorMessage.value = it; isLoading.value = false }
        )
    }

    fun fetchBillsByDateRange(from: Long, to: Long) {
        isLoading.value = true
        repository.getBillsByDateRange(from, to,
            onResult = { billList.value = it; isLoading.value = false },
            onError = { errorMessage.value = it; isLoading.value = false }
        )
    }

    // FIX: uses actual timestamp stored in the bill, not a string prefix
    fun getTodaySummary(): Triple<Double, Double, Double> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayBills = billList.value.filter { it.timestamp >= todayStart }
        return Triple(
            todayBills.sumOf { it.itemsTotal },
            todayBills.sumOf { it.paidAmount },
            todayBills.sumOf { it.balance }
        )
    }
}