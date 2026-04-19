package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.ProductReport
import com.animan.wholesalemanager.repository.CustomerRepository
import com.animan.wholesalemanager.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.*

class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val billRepo = CustomerRepository(app.applicationContext)
    private val expenseRepo = ExpenseRepository(app.applicationContext)

    var topProducts = mutableStateOf<List<ProductReport>>(emptyList())
    var dailySales = mutableStateOf<List<Pair<String, Double>>>(emptyList())
    var totalSales = mutableStateOf(0.0)
    var todaySales = mutableStateOf(0.0)
    var totalBills = mutableStateOf(0)
    var totalExpense = mutableStateOf(0.0)
    var profit = mutableStateOf(0.0)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun fetchReport() {
        isLoading.value = true

        expenseRepo.getExpenses(
            onResult = { expenses ->
                totalExpense.value = expenses.sumOf { it.amount }

                billRepo.getBills(
                    onResult = { bills ->
                        totalBills.value = bills.size
                        totalSales.value = bills.sumOf { it.itemsTotal }

                        // FIX: use actual timestamp, not ID prefix
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        todaySales.value = bills
                            .filter { it.timestamp >= todayStart }
                            .sumOf { it.itemsTotal }

                        profit.value = totalSales.value - totalExpense.value
                        isLoading.value = false
                    },
                    onError = { errorMessage.value = it; isLoading.value = false }
                )
            },
            onError = { errorMessage.value = it; isLoading.value = false }
        )
    }

    fun fetchTopProducts() {
        billRepo.getBills(
            onResult = { bills ->
                val map = mutableMapOf<String, ProductReport>()
                bills.forEach { bill ->
                    bill.items.forEach { item ->
                        val existing = map[item.productId]
                        map[item.productId] = if (existing != null) {
                            existing.copy(
                                totalQty = existing.totalQty + item.quantity,
                                totalRevenue = existing.totalRevenue + (item.price * item.quantity)
                            )
                        } else {
                            ProductReport(
                                productId = item.productId,
                                name = item.name,
                                totalQty = item.quantity,
                                totalRevenue = item.price * item.quantity
                            )
                        }
                    }
                }
                topProducts.value = map.values.sortedByDescending { it.totalQty }
            },
            onError = { errorMessage.value = it }
        )
    }

    fun fetchDailySales() {
        billRepo.getBills(
            onResult = { bills ->
                val map = mutableMapOf<String, Double>()
                bills.forEach { bill ->
                    // FIX: format real timestamp into a readable date string
                    val dateKey = dateFormat.format(Date(bill.timestamp))
                    map[dateKey] = (map[dateKey] ?: 0.0) + bill.itemsTotal
                }
                // sort by date ascending
                dailySales.value = map.toList().sortedBy {
                    dateFormat.parse(it.first)?.time ?: 0L
                }
            },
            onError = { errorMessage.value = it }
        )
    }
}