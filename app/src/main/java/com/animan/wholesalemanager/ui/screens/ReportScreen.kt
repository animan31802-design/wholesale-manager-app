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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import com.animan.wholesalemanager.viewmodel.ReportViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*

@Composable
fun ReportScreen() {

    val viewModel: ReportViewModel = viewModel()

    var selectedFilter by remember { mutableStateOf("Today") }

    LaunchedEffect(Unit) {
        viewModel.fetchReport()
        viewModel.fetchTopProducts()
        viewModel.fetchDailySales()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        LazyColumn {

            // 🔥 HEADER
            item { ReportHeader() }

            // 🔥 CONTENT
            item {
                Column(modifier = Modifier.padding(16.dp)) {

                    if (viewModel.isLoading.value) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ReportFilters(selected = selectedFilter) { selected ->
                        selectedFilter = selected
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // KPI GRID
                    KPISection(viewModel)

                    Spacer(modifier = Modifier.height(16.dp))

                    InsightCard(viewModel)

                    Spacer(modifier = Modifier.height(20.dp))

                    PremiumSection("Daily Sales") {
                        DailySalesLineChart(viewModel.dailySales.value)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    PremiumSection("Top Products Chart") {
                        TopProductsBarChart(viewModel.topProducts.value)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Top Products",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            items(viewModel.topProducts.value) { product ->
                PremiumProductItem(product)
            }
            item {
                viewModel.errorMessage.value?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun KPISection(viewModel: ReportViewModel) {

    Column {

        Row {
            PremiumReportCard(
                "Total Sales",
                viewModel.totalSales.value,
                Icons.Default.AttachMoney,
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            PremiumReportCard(
                "Today",
                viewModel.todaySales.value,
                Icons.Default.Today,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row {
            PremiumReportCard(
                "Profit",
                viewModel.profit.value,
                Icons.Default.TrendingUp,
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            PremiumReportCard(
                "Expense",
                viewModel.totalExpense.value,
                Icons.Default.Warning,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ReportHeader() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .padding(20.dp)
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Text(
                "Reports Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                "Your business insights",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ReportFilters(selected: String, onSelect: (String) -> Unit) {

    val filters = listOf("Today", "Week", "Month")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        filters.forEach { label ->

            FilterChip(
                selected = selected == label,
                onClick = { onSelect(label) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun AnimatedNumber(target: Double): String {

    val animatedValue by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(1000)
    )

    return "₹${animatedValue.toInt()}"
}

@Composable
fun PremiumReportCard(
    title: String,
    value: Double,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {

    val animatedValue = AnimatedNumber(value)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(16.dp)
        ) {

            Icon(icon, contentDescription = null)

            Spacer(modifier = Modifier.height(8.dp))

            Text(title, style = MaterialTheme.typography.bodySmall)

            Text(
                animatedValue,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun InsightCard(viewModel: ReportViewModel) {

    val profit = viewModel.profit.value
    val today = viewModel.todaySales.value

    val message = when {
        profit > 0 -> "You're making profit 📈"
        profit < 0 -> "Loss detected ⚠️"
        else -> "No profit yet"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text("Insights", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(6.dp))

            Text(message)

            Text("Today's Sales: ₹$today")
        }
    }
}

@Composable
fun PremiumSection(title: String, content: @Composable () -> Unit) {

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {

            Text(title, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PremiumProductItem(product: ProductReport) {

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("Sold: ${product.totalQty}")
            }

            Text(
                "₹${product.totalRevenue}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun DailySalesLineChart(dataList: List<Pair<String, Double>>) {

    AndroidView<LineChart>(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
            }
        },
        update = { chart ->

            val entries = dataList.mapIndexed { index, pair ->
                Entry(index.toFloat(), pair.second.toFloat())
            }

            val labels = dataList.map { it.first } // dates

            val dataSet = LineDataSet(entries, "").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER   // smooth curve
                setDrawCircles(false)
                lineWidth = 3f
            }
            val data = LineData(dataSet)

            chart.data = data

            // 🔥 X-Axis Labels (Dates)
            val xAxis = chart.xAxis
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.labelRotationAngle = -45f
            xAxis.valueFormatter =
                com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun TopProductsBarChart(products: List<ProductReport>) {

    AndroidView<BarChart>(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                animateY(1000)
            }
        },
        update = { chart ->

            val entries = products.mapIndexed { index, product ->
                BarEntry(index.toFloat(), product.totalQty.toFloat())
            }

            val labels = products.map { it.name }

            val dataSet = BarDataSet(entries, "Top Products")
            val data = BarData(dataSet)

            chart.data = data

            // 🔥 X-Axis Labels (Product Names)
            val xAxis = chart.xAxis
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.labelRotationAngle = -45f
            xAxis.valueFormatter =
                com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)

            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}