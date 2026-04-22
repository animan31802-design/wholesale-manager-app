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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.ProductReport
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.ReportViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*

@Composable
fun ReportScreen() {
    val viewModel: ReportViewModel = viewModel()
    var selectedFilter by remember { mutableStateOf("All") }

    // FIX 2: re-fetch everything whenever the filter chip changes
    LaunchedEffect(selectedFilter) {
        viewModel.fetchReport(selectedFilter)
        viewModel.fetchTopProducts(selectedFilter)
        viewModel.fetchDailySales(selectedFilter)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn {
            item { ReportHeader() }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (viewModel.isLoading.value) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    // FIX 2: filters now include "All"
                    ReportFilters(selected = selectedFilter) { selectedFilter = it }

                    Spacer(Modifier.height(12.dp))
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
            }
        }
    }
}

@Composable
fun ReportFilters(selected: String, onSelect: (String) -> Unit) {
    // FIX 2: "All" added as default filter
    val filters = listOf("All", "Today", "Week", "Month")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())) {
        filters.forEach { label ->
            FilterChip(selected = selected == label, onClick = { onSelect(label) },
                label = { Text(label) })
        }
    }
}

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
            // FIX 7: show gross profit (margin) not revenue - expenses
            PremiumReportCard("Gross profit", viewModel.grossProfit.value,
                Icons.Default.TrendingUp, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            PremiumReportCard("Net profit", viewModel.netProfit.value,
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

@Composable
fun AnimatedNumber(target: Double): String {
    val v by animateFloatAsState(targetValue = target.toFloat(), animationSpec = tween(1000),
        label = "money")
    return "₹${"%.2f".format(v)}"
}

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

@Composable
fun PremiumSection(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

@Composable
fun PremiumProductItem(product: ProductReport) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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

@Composable
fun DailySalesLineChart(dataList: List<Pair<String, Double>>) {
    AndroidView<LineChart>(
        factory = { ctx -> LineChart(ctx).apply { description.isEnabled = false } },
        update = { chart ->
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
        update = { chart ->
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