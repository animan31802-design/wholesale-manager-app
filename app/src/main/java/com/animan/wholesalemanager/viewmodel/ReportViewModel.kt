package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.local.ProductReport
import com.animan.wholesalemanager.repository.CustomerRepository
import java.text.SimpleDateFormat
import java.util.*

class ReportViewModel : ViewModel() {

    private val repository = CustomerRepository()
    var topProducts = mutableStateOf<List<ProductReport>>(emptyList())
    var dailySales = mutableStateOf<List<Pair<String, Double>>>(emptyList())

    var totalSales = mutableStateOf(0.0)
    var todaySales = mutableStateOf(0.0)
    var totalBills = mutableStateOf(0)

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    var totalExpense = mutableStateOf(0.0)
    var profit = mutableStateOf(0.0)

    fun fetchReport() {

        isLoading.value = true

        profit.value = totalSales.value - totalExpense.value

        repository.getBillsSummary(
            onResult = { bills ->

                totalBills.value = bills.size

                totalSales.value = bills.sumOf { it.itemsTotal }

                val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    .format(Date())

                todaySales.value = bills.filter {
                    it.id.take(8) == today // simple date logic
                }.sumOf { it.itemsTotal }

                isLoading.value = false
            },
            onError = {
                errorMessage.value = it
                isLoading.value = false
            }
        )
    }

    fun fetchTopProducts() {

        isLoading.value = true

        profit.value = totalSales.value - totalExpense.value

        repository.getBillsSummary(
            onResult = { bills ->

                val map = mutableMapOf<String, ProductReport>()

                bills.forEach { bill ->

                    bill.items.forEach { item ->

                        val existing = map[item.productId]

                        if (existing != null) {
                            map[item.productId] = existing.copy(
                                totalQty = existing.totalQty + item.quantity,
                                totalRevenue = existing.totalRevenue + (item.price * item.quantity)
                            )
                        } else {
                            map[item.productId] = ProductReport(
                                productId = item.productId,
                                name = item.name,
                                totalQty = item.quantity,
                                totalRevenue = item.price * item.quantity
                            )
                        }
                    }
                }

                // 🔥 Sort by highest quantity sold
                topProducts.value = map.values
                    .sortedByDescending { it.totalQty }

                isLoading.value = false
            },
            onError = {
                errorMessage.value = it
                isLoading.value = false
            }
        )
    }

    fun fetchDailySales() {

        profit.value = totalSales.value - totalExpense.value

        repository.getBillsSummary(
            onResult = { bills ->

                val map = mutableMapOf<String, Double>()

                bills.forEach { bill ->

                    val date = bill.id.take(8) // temporary date

                    val existing = map[date] ?: 0.0
                    map[date] = existing + bill.itemsTotal
                }

                // sort by date
                dailySales.value = map.toList().sortedBy { it.first }
            },
            onError = {
                errorMessage.value = it
            }
        )
    }
}