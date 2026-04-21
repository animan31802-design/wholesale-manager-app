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

    private val billRepo    = CustomerRepository(app.applicationContext)
    private val expenseRepo = ExpenseRepository(app.applicationContext)

    var topProducts  = mutableStateOf<List<ProductReport>>(emptyList())
    var dailySales   = mutableStateOf<List<Pair<String, Double>>>(emptyList())
    var totalSales   = mutableStateOf(0.0)
    var todaySales   = mutableStateOf(0.0)
    var totalBills   = mutableStateOf(0)
    var totalExpense = mutableStateOf(0.0)
    var grossProfit  = mutableStateOf(0.0)   // margin on sales
    var netProfit    = mutableStateOf(0.0)   // gross - expenses
    // Keep "profit" alias so existing UI doesn't break
    val profit get() = netProfit

    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Round to 2 decimal places to avoid floating point noise like 0.2000000045
    private fun Double.roundMoney() = Math.round(this * 100) / 100.0

    // ── Date range helpers ────────────────────────────────────────────
    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return start to cal.timeInMillis
    }

    private fun weekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to System.currentTimeMillis()
    }

    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return start to System.currentTimeMillis()
    }

    // ── Main fetch — called when filter chip changes ──────────────────
    fun fetchReport(filter: String = "All") {
        isLoading.value = true

        val (from, to) = when (filter) {
            "Today" -> todayRange()
            "Week"  -> weekRange()
            "Month" -> monthRange()
            else    -> 0L to System.currentTimeMillis()
        }

        expenseRepo.getExpenses(
            onResult = { expenses ->
                val expenseTotal = expenses
                    .filter { filter == "All" || it.date in from..to }
                    .sumOf { it.amount }
                    .roundMoney()
                totalExpense.value = expenseTotal

                val billFetch: (onResult: (List<com.animan.wholesalemanager.data.local.Bill>) -> Unit,
                                onError: (String) -> Unit) -> Unit =
                    if (filter == "All") billRepo::getBills
                    else { onResult, onError ->
                        billRepo.getBillsByDateRange(from, to, onResult, onError)
                    }

                billFetch(
                    { bills ->
                        val activeBills = bills.filter { !it.isRefunded }
                        totalBills.value  = activeBills.size
                        totalSales.value  = activeBills.sumOf { it.grandTotal }.roundMoney()

                        // Today's sales within the fetched set
                        val (tStart, tEnd) = todayRange()
                        todaySales.value = activeBills
                            .filter { it.timestamp in tStart..tEnd }
                            .sumOf { it.grandTotal }.roundMoney()

                        // FIX 7: gross profit = sum of (sellingPrice - costPrice) * qty
                        val gross = activeBills.sumOf { bill ->
                            bill.items.sumOf { item ->
                                ((item.price - item.costPrice) * item.quantity).roundMoney()
                            }
                        }.roundMoney()
                        grossProfit.value = gross
                        netProfit.value   = (gross - expenseTotal).roundMoney()

                        isLoading.value = false
                    },
                    { error -> errorMessage.value = error; isLoading.value = false }
                )
            },
            onError = { errorMessage.value = it; isLoading.value = false }
        )
    }

    fun fetchTopProducts(filter: String = "All") {
        val (from, to) = when (filter) {
            "Today" -> todayRange()
            "Week"  -> weekRange()
            "Month" -> monthRange()
            else    -> 0L to System.currentTimeMillis()
        }

        val fetch: (onResult: (List<com.animan.wholesalemanager.data.local.Bill>) -> Unit,
                    onError: (String) -> Unit) -> Unit =
            if (filter == "All") billRepo::getBills
            else { onResult, onError -> billRepo.getBillsByDateRange(from, to, onResult, onError) }

        fetch(
            { bills ->
                val map = mutableMapOf<String, ProductReport>()
                bills.filter { !it.isRefunded }.forEach { bill ->
                    bill.items.forEach { item ->
                        val existing = map[item.productId]
                        map[item.productId] = if (existing != null) {
                            existing.copy(
                                totalQty     = existing.totalQty + item.quantity,
                                totalRevenue = (existing.totalRevenue + item.price * item.quantity).roundMoney()
                            )
                        } else {
                            ProductReport(item.productId, item.name, item.quantity,
                                (item.price * item.quantity).roundMoney())
                        }
                    }
                }
                topProducts.value = map.values.sortedByDescending { it.totalQty }
            },
            { errorMessage.value = it }
        )
    }

    fun fetchDailySales(filter: String = "All") {
        val (from, to) = when (filter) {
            "Today" -> todayRange()
            "Week"  -> weekRange()
            "Month" -> monthRange()
            else    -> 0L to System.currentTimeMillis()
        }

        val fetch: (onResult: (List<com.animan.wholesalemanager.data.local.Bill>) -> Unit,
                    onError: (String) -> Unit) -> Unit =
            if (filter == "All") billRepo::getBills
            else { onResult, onError -> billRepo.getBillsByDateRange(from, to, onResult, onError) }

        fetch(
            { bills ->
                val map = mutableMapOf<String, Double>()
                bills.filter { !it.isRefunded }.forEach { bill ->
                    val key = dateFormat.format(Date(bill.timestamp))
                    map[key] = ((map[key] ?: 0.0) + bill.grandTotal).roundMoney()
                }
                dailySales.value = map.toList().sortedBy {
                    dateFormat.parse(it.first)?.time ?: 0L
                }
            },
            { errorMessage.value = it }
        )
    }
}