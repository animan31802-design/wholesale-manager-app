package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        viewModel.fetchReport()
        viewModel.fetchTopProducts()
        viewModel.fetchDailySales()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔹 Title
        item {
            Text("Reports", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 🔹 Loading
        if (viewModel.isLoading.value) {
            item {
                CircularProgressIndicator()
            }
        }

        // 🔹 Summary Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Sales: ₹${viewModel.totalSales.value}")
                    Text("Today's Sales: ₹${viewModel.todaySales.value}")
                    Text("Total Bills: ${viewModel.totalBills.value}")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Expense: ₹${viewModel.totalExpense.value}")
                    Text("Profit: ₹${viewModel.profit.value}")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 🔹 Top Products List
        item {
            Text("Top Selling Products", style = MaterialTheme.typography.titleMedium)
        }

        items(viewModel.topProducts.value) { product ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {

                Column(modifier = Modifier.padding(10.dp)) {

                    Text(product.name)
                    Text("Sold: ${product.totalQty}")
                    Text("Revenue: ₹${product.totalRevenue}")
                }
            }
        }

        // Chart: Top Products
        item {
            Spacer(modifier = Modifier.height(20.dp))

            Text("Top Products Chart")

            TopProductsBarChart(viewModel.topProducts.value)
        }

        // Chart: Daily Sales
        item {
            Spacer(modifier = Modifier.height(20.dp))

            Text("Daily Sales Chart")

            DailySalesLineChart(viewModel.dailySales.value)
        }

        // 🔹 Error
        item {
            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
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

            val dataSet = LineDataSet(entries, "Daily Sales")
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