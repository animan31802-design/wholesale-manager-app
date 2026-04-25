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
    var grossProfit  = mutableStateOf(0.0)
    var netProfit    = mutableStateOf(0.0)
    val profit get() = netProfit

    var isLoading    = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    // ── Navigation offset — 0 = current period, -1 = one period back ──
    var periodOffset = mutableStateOf(0)

    // ── Custom date range ─────────────────────────────────────────────
    var customFromDate = mutableStateOf<Long?>(null)
    var customToDate   = mutableStateOf<Long?>(null)

    // ── Active filter ─────────────────────────────────────────────────
    var currentFilter = mutableStateOf("All")

    private val dateFormat      = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayFmt      = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val displayFmtShort = SimpleDateFormat("dd MMM", Locale.getDefault())

    private fun Double.roundMoney() = Math.round(this * 100) / 100.0

    // ── Navigation ────────────────────────────────────────────────────

    fun navigatePrevious() {
        if (currentFilter.value in listOf("Today", "Week", "Month", "Year")) {
            periodOffset.value -= 1
            fetchAll(currentFilter.value)
        }
    }

    fun navigateNext() {
        if (canNavigateNext) {
            periodOffset.value += 1
            fetchAll(currentFilter.value)
        }
    }

    val canNavigateNext: Boolean
        get() = currentFilter.value in listOf("Today", "Week", "Month", "Year")
                && periodOffset.value < 0

    val showNavArrows: Boolean
        get() = currentFilter.value in listOf("Today", "Week", "Month", "Year")

    // ── Period label for the header ───────────────────────────────────
    fun periodLabel(): String {
        val offset = periodOffset.value
        return when (currentFilter.value) {
            "All"    -> "All Time"
            "Custom" -> {
                val from = customFromDate.value ?: return "Custom Range"
                val to   = customToDate.value   ?: return "Custom Range"
                "${displayFmtShort.format(Date(from))} – ${displayFmtShort.format(Date(to))}"
            }
            "Today"  -> when (offset) {
                0    -> "Today"
                -1   -> "Yesterday"
                else -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
                    displayFmt.format(cal.time)
                }
            }
            "Week"   -> {
                val (from, to) = weekRange(offset)
                val label = "${displayFmtShort.format(Date(from))} – ${displayFmtShort.format(Date(to))}"
                if (offset == 0) "This Week  ($label)" else label
            }
            "Month"  -> {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
            }
            "Year"   -> {
                val cal = Calendar.getInstance().apply { add(Calendar.YEAR, offset) }
                SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
            }
            else -> ""
        }
    }

    // ── Date range helpers (offset-aware) ────────────────────────────

    private fun todayRange(offset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    private fun weekRange(offset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, offset) }
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val endCal = cal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_WEEK, 6)
        endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999)
        return start to endCal.timeInMillis
    }

    private fun monthRange(offset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999)
        return start to endCal.timeInMillis
    }

    private fun yearRange(offset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, offset) }
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_YEAR, endCal.getActualMaximum(Calendar.DAY_OF_YEAR))
        endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59); endCal.set(Calendar.MILLISECOND, 999)
        return start to endCal.timeInMillis
    }

    private fun customRange(): Pair<Long, Long> {
        val from = customFromDate.value ?: 0L
        val to   = customToDate.value?.let { endOfDay(it) } ?: System.currentTimeMillis()
        return from to to
    }

    private fun endOfDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun rangeFor(filter: String): Pair<Long, Long> {
        val offset = periodOffset.value
        return when (filter) {
            "Today"  -> todayRange(offset)
            "Week"   -> weekRange(offset)
            "Month"  -> monthRange(offset)
            "Year"   -> yearRange(offset)
            "Custom" -> customRange()
            else     -> 0L to System.currentTimeMillis()
        }
    }

    // ── Public entry points ───────────────────────────────────────────

    /** Fetch all 3 data sets for the given filter */
    fun fetchAll(filter: String = currentFilter.value) {
        currentFilter.value = filter
        fetchReport(filter)
        fetchTopProducts(filter)
        fetchDailySales(filter)
    }

    /** User tapped a filter chip — reset offset to 0 (current period) */
    fun selectFilter(filter: String) {
        periodOffset.value = 0
        fetchAll(filter)
    }

    /** User confirmed a custom date range in the picker dialog */
    fun applyCustomRange(fromMillis: Long, toMillis: Long) {
        customFromDate.value = fromMillis
        customToDate.value   = toMillis
        periodOffset.value   = 0
        fetchAll("Custom")
    }

    // ── Internal fetch methods ────────────────────────────────────────

    fun fetchReport(filter: String = currentFilter.value) {
        isLoading.value = true
        val (from, to) = rangeFor(filter)

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
                        totalBills.value = activeBills.size
                        totalSales.value = activeBills.sumOf { it.grandTotal }.roundMoney()

                        val (tStart, tEnd) = todayRange(0)
                        todaySales.value = activeBills
                            .filter { it.timestamp in tStart..tEnd }
                            .sumOf { it.grandTotal }.roundMoney()

                        val gross = activeBills.sumOf { bill ->
                            bill.items.sumOf { item ->
                                ((item.price - item.costPrice) * item.quantity).roundMoney()
                            }
                        }.roundMoney()
                        grossProfit.value = gross
                        netProfit.value   = (gross - expenseTotal).roundMoney()
                        isLoading.value   = false
                    },
                    { error -> errorMessage.value = error; isLoading.value = false }
                )
            },
            onError = { errorMessage.value = it; isLoading.value = false }
        )
    }

    fun fetchTopProducts(filter: String = currentFilter.value) {
        val (from, to) = rangeFor(filter)
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

    fun fetchDailySales(filter: String = currentFilter.value) {
        val (from, to) = rangeFor(filter)
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