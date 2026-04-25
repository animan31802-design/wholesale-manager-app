package com.animan.wholesalemanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.ProductReport
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.ReportViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import java.text.SimpleDateFormat
import java.util.*

// ── Main Screen ───────────────────────────────────────────────────────

@Composable
fun ReportScreen() {
    val viewModel: ReportViewModel = viewModel()
    var showDatePicker by remember { mutableStateOf(false) }

    // Initial load
    LaunchedEffect(Unit) { viewModel.fetchAll("All") }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn {
            item { ReportHeader() }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(12.dp))

                    // ── Filter chips ──────────────────────────────────
                    ReportFilters(
                        selected      = viewModel.currentFilter.value,
                        customLabel   = if (viewModel.currentFilter.value == "Custom")
                            viewModel.periodLabel() else null,
                        onSelect      = { filter ->
                            if (filter == "Custom") showDatePicker = true
                            else viewModel.selectFilter(filter)
                        }
                    )

                    // ── Period navigation bar ─────────────────────────
                    if (viewModel.showNavArrows) {
                        Spacer(Modifier.height(8.dp))
                        PeriodNavigationBar(
                            label       = viewModel.periodLabel(),
                            canGoNext   = viewModel.canNavigateNext,
                            onPrevious  = { viewModel.navigatePrevious() },
                            onNext      = { viewModel.navigateNext() }
                        )
                    } else if (viewModel.currentFilter.value != "All") {
                        // Show label-only bar for Custom
                        Spacer(Modifier.height(8.dp))
                        PeriodLabelBar(label = viewModel.periodLabel())
                    }

                    Spacer(Modifier.height(12.dp))

                    if (viewModel.isLoading.value) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    KPISection(viewModel)
                    Spacer(Modifier.height(16.dp))
                    InsightCard(viewModel)
                    Spacer(Modifier.height(20.dp))
                    PremiumSection("Daily Sales") { DailySalesLineChart(viewModel.dailySales.value) }
                    Spacer(Modifier.height(20.dp))
                    PremiumSection("Top Products Chart") { TopProductsBarChart(viewModel.topProducts.value) }
                    Spacer(Modifier.height(20.dp))
                    Text("Top Products", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                }
            }

            items(viewModel.topProducts.value) { product -> PremiumProductItem(product) }

            item {
                viewModel.errorMessage.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── Custom date range picker dialog ───────────────────────────────
    if (showDatePicker) {
        CustomDateRangePickerDialog(
            initialFrom = viewModel.customFromDate.value,
            initialTo   = viewModel.customToDate.value,
            onConfirm   = { from, to ->
                showDatePicker = false
                viewModel.applyCustomRange(from, to)
            },
            onDismiss   = {
                showDatePicker = false
                // If user cancels without ever picking custom, revert to previous filter
                if (viewModel.currentFilter.value != "Custom") return@CustomDateRangePickerDialog
            }
        )
    }
}

// ── Filter chips ──────────────────────────────────────────────────────

@Composable
fun ReportFilters(
    selected: String,
    customLabel: String?,
    onSelect: (String) -> Unit
) {
    val filters = listOf("All", "Today", "Week", "Month", "Year", "Custom")
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        filters.forEach { label ->
            val displayLabel = if (label == "Custom" && customLabel != null) customLabel else label
            FilterChip(
                selected = selected == label,
                onClick  = { onSelect(label) },
                label    = { Text(displayLabel, maxLines = 1) },
                leadingIcon = if (label == "Custom") {
                    { Icon(Icons.Default.DateRange, null, Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

// ── Period navigation bar (prev / label / next) ───────────────────────

@Composable
fun PeriodNavigationBar(
    label: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous period",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                text  = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next period",
                    tint = if (canGoNext)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun PeriodLabelBar(label: String) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ── Custom date range picker dialog ──────────────────────────────────
// Uses Android's DatePickerDialog since Material3 DateRangePicker requires
// Activity-level theming. Two sequential pickers: From → To.

@Composable
fun CustomDateRangePickerDialog(
    initialFrom: Long?,
    initialTo: Long?,
    onConfirm: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var fromDate by remember { mutableStateOf<Long?>(initialFrom) }
    var toDate   by remember { mutableStateOf<Long?>(initialTo) }
    var step     by remember { mutableStateOf(0) } // 0 = pick from, 1 = pick to, 2 = confirm

    val displayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Step 0: pick From date
    // Step 1: pick To date
    // Step 2: show confirm dialog
    when (step) {
        0 -> {
            NativeDatePickerDialog(
                title        = "Select Start Date",
                initialMillis = fromDate ?: System.currentTimeMillis(),
                maxMillis    = toDate,       // from can't be after existing to
                onDatePicked = { millis -> fromDate = millis; step = 1 },
                onDismiss    = onDismiss
            )
        }
        1 -> {
            NativeDatePickerDialog(
                title         = "Select End Date",
                initialMillis = toDate ?: fromDate ?: System.currentTimeMillis(),
                minMillis     = fromDate,    // to must be on/after from
                onDatePicked  = { millis -> toDate = millis; step = 2 },
                onDismiss     = { step = 0 } // go back to from picker
            )
        }
        2 -> {
            // Summary confirm dialog
            AlertDialog(
                onDismissRequest = onDismiss,
                title   = { Text("Confirm Date Range") },
                text    = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("From:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                displayFmt.format(Date(fromDate!!)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("To:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                displayFmt.format(Date(toDate!!)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onConfirm(fromDate!!, toDate!!) }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { step = 1 }) { Text("Change To") }
                        TextButton(onClick = { step = 0 }) { Text("Change From") }
                    }
                }
            )
        }
    }
}

// ── Native DatePickerDialog wrapper ──────────────────────────────────

@Composable
fun NativeDatePickerDialog(
    title: String,
    initialMillis: Long,
    minMillis: Long? = null,
    maxMillis: Long? = null,
    onDatePicked: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cal     = Calendar.getInstance().apply { timeInMillis = initialMillis }

    DisposableEffect(title) {
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                }
                onDatePicked(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.setTitle(title)
        dialog.setOnCancelListener { onDismiss() }
        minMillis?.let { dialog.datePicker.minDate = it }
        maxMillis?.let { dialog.datePicker.maxDate = it }
        dialog.show()

        onDispose { if (dialog.isShowing) dialog.dismiss() }
    }
}

// ── KPI cards ─────────────────────────────────────────────────────────

@Composable
fun KPISection(viewModel: ReportViewModel) {
    Column {
        Row {
            PremiumReportCard("Total Sales", viewModel.totalSales.value,
                Icons.Default.AttachMoney, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PremiumReportCard("Today", viewModel.todaySales.value,
                Icons.Default.Today, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            PremiumReportCard("Gross Profit", viewModel.grossProfit.value,
                Icons.Default.TrendingUp, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PremiumReportCard("Net Profit", viewModel.netProfit.value,
                Icons.Default.AccountBalance, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row {
            PremiumReportCard("Expenses", viewModel.totalExpense.value,
                Icons.Default.Warning, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PremiumReportCard("Bills", viewModel.totalBills.value.toDouble(),
                Icons.Default.Receipt, Modifier.weight(1f))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────

@Composable
fun ReportHeader() {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp)
        .background(Brush.linearGradient(listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary)))
        .padding(20.dp)) {
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
            Text("Reports Dashboard", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary)
            Text("Your business insights",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }
    }
}

// ── Animated number ───────────────────────────────────────────────────

@Composable
fun AnimatedNumber(target: Double): String {
    val v by animateFloatAsState(targetValue = target.toFloat(), animationSpec = tween(1000),
        label = "money")
    return "₹${"%.2f".format(v)}"
}

// ── Report card ───────────────────────────────────────────────────────

@Composable
fun PremiumReportCard(title: String, value: Double, icon: ImageVector, modifier: Modifier = Modifier) {
    val animated = AnimatedNumber(value)
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier
            .background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant)))
            .padding(16.dp)) {
            Icon(icon, null)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(animated, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// ── Insight card ──────────────────────────────────────────────────────

@Composable
fun InsightCard(viewModel: ReportViewModel) {
    val gross = viewModel.grossProfit.value
    val net   = viewModel.netProfit.value
    val message = when {
        net > 0   -> "Net profit: ${net.toRupees()} after expenses"
        net < 0   -> "Net loss of ${(-net).toRupees()} — check expenses"
        gross > 0 -> "Sales margin is positive but expenses offset it"
        else      -> "No sales recorded yet"
    }
    Card(shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Insights", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(message)
            Spacer(Modifier.height(4.dp))
            Text("Gross profit (margin): ${viewModel.grossProfit.value.toRupees()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Today's sales: ${viewModel.todaySales.value.toRupees()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────

@Composable
fun PremiumSection(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

// ── Product item ──────────────────────────────────────────────────────

@Composable
fun PremiumProductItem(product: ProductReport) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("Sold: ${product.totalQty}", style = MaterialTheme.typography.bodySmall)
            }
            Text(product.totalRevenue.toRupees(), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Charts ────────────────────────────────────────────────────────────

@Composable
fun DailySalesLineChart(dataList: List<Pair<String, Double>>) {
    AndroidView<LineChart>(
        factory = { ctx -> LineChart(ctx).apply { description.isEnabled = false } },
        update  = { chart ->
            if (dataList.isEmpty()) { chart.clear(); return@AndroidView }
            val entries = dataList.mapIndexed { i, p -> Entry(i.toFloat(), p.second.toFloat()) }
            val labels  = dataList.map { it.first }
            val ds = LineDataSet(entries, "").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER; setDrawCircles(false); lineWidth = 3f
            }
            chart.data = LineData(ds)
            chart.xAxis.apply {
                granularity = 1f; setDrawGridLines(false); labelRotationAngle = -45f
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(300.dp)
    )
}

@Composable
fun TopProductsBarChart(products: List<ProductReport>) {
    AndroidView<BarChart>(
        factory = { ctx -> BarChart(ctx).apply { description.isEnabled = false; animateY(1000) } },
        update  = { chart ->
            if (products.isEmpty()) { chart.clear(); return@AndroidView }
            val entries = products.mapIndexed { i, p -> BarEntry(i.toFloat(), p.totalQty.toFloat()) }
            val labels  = products.map { it.name }
            chart.data  = BarData(BarDataSet(entries, "Top Products"))
            chart.xAxis.apply {
                granularity = 1f; setDrawGridLines(false); labelRotationAngle = -45f
                valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            }
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(300.dp)
    )
}