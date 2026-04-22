package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.repository.CustomerRepository
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import java.util.Calendar

class BillViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = CustomerRepository(app.applicationContext)

    var billList     = mutableStateOf<List<Bill>>(emptyList())
    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun fetchBills() {
        isLoading.value = true
        repository.getBills(
            onResult = { billList.value = it; isLoading.value = false },
            onError  = { errorMessage.value = it; isLoading.value = false }
        )
    }

    fun fetchBillsByDateRange(from: Long, to: Long) {
        isLoading.value = true
        repository.getBillsByDateRange(from, to,
            onResult = { billList.value = it; isLoading.value = false },
            onError  = { errorMessage.value = it; isLoading.value = false }
        )
    }

    // effectiveTotal: use grandTotal if it was saved (Phase 4+),
    // fall back to itemsTotal for older bills that have grandTotal=0
    private fun Bill.effectiveTotal(): Double =
        if (grandTotal > 0.0) grandTotal else itemsTotal

    fun getTodaySummary(): Triple<Double, Double, Double> {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val today = billList.value.filter {
            it.timestamp >= todayStart && !it.isRefunded
        }

        val totalBilled   = today.sumOf { it.effectiveTotal() }.round2dp()
        val totalPaid     = today.sumOf { it.paidAmount }.round2dp()
        // Calculate due from billed - paid, NOT from stored balance field
        // (stored balance can be stale or have floating point issues)
        val totalDue      = (totalBilled - totalPaid).round2dp()

        return Triple(totalBilled, totalPaid, totalDue)
    }

    // For dashboard overall — returns consistent set of numbers
    data class OverallSummary(
        val totalBilled: Double,
        val totalPaid: Double,
        val totalDue: Double
    )

    fun getOverallSummary(): OverallSummary {
        val active = billList.value.filter { !it.isRefunded }
        val billed = active.sumOf { it.effectiveTotal() }.round2dp()
        val paid   = active.sumOf { it.paidAmount }.round2dp()
        val due    = (billed - paid).round2dp()
        return OverallSummary(billed, paid, due)
    }
}