package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Purchase
import com.animan.wholesalemanager.data.local.Supplier
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import com.animan.wholesalemanager.viewmodel.SupplierViewModel
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierReportScreen(navController: NavController) {

    val viewModel: SupplierViewModel = viewModel()

    // Date range state — default to current month
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val defaultFrom = calendar.timeInMillis
    val defaultTo   = System.currentTimeMillis()

    var fromMs       by remember { mutableStateOf(defaultFrom) }
    var toMs         by remember { mutableStateOf(defaultTo) }
    var selectedRange by remember { mutableStateOf("This month") }

    val rangeOptions = listOf("This month", "Last month", "Last 3 months", "All time")

    LaunchedEffect(Unit) {
        viewModel.fetchSuppliers()
        viewModel.fetchAllPurchases()
    }

    // Filtered purchases by date range
    val filteredPurchases by remember(viewModel.purchaseList.value, fromMs, toMs) {
        derivedStateOf {
            viewModel.purchaseList.value.filter {
                it.timestamp in fromMs..toMs
            }
        }
    }

    // Per-supplier summary
    val supplierSummaries by remember(filteredPurchases, viewModel.supplierList.value) {
        derivedStateOf {
            filteredPurchases
                .groupBy { it.supplierId }
                .map { (supplierId, purchases) ->
                    val supplier = viewModel.supplierList.value.find { it.id == supplierId }
                    SupplierSummary(
                        supplier      = supplier,
                        supplierName  = purchases.first().supplierName,
                        totalSpent    = purchases.sumOf { it.grandTotal },
                        totalPaid     = purchases.sumOf { it.paidAmount },
                        totalDue      = purchases.sumOf { it.balance },
                        purchaseCount = purchases.size,
                        purchases     = purchases
                    )
                }
                .sortedByDescending { it.totalSpent }
        }
    }

    val grandTotalSpent = filteredPurchases.sumOf { it.grandTotal }
    val grandTotalPaid  = filteredPurchases.sumOf { it.paidAmount }
    val grandTotalDue   = filteredPurchases.sumOf { it.balance }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier report") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Date range selector ───────────────────────────────────
            item {
                Text("Date range",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    rangeOptions.forEach { option ->
                        FilterChip(
                            selected = selectedRange == option,
                            onClick  = {
                                selectedRange = option
                                val (from, to) = getDateRange(option)
                                fromMs = from
                                toMs   = to
                            },
                            label    = { Text(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Grand total summary card ──────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Total purchases — $selectedRange",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.2f))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total spent")
                            Text(grandTotalSpent.toRupees(),
                                fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total paid",
                                color = MaterialTheme.colorScheme.primary)
                            Text(grandTotalPaid.toRupees(),
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium)
                        }
                        if (grandTotalDue > 0) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Total due",
                                    color = MaterialTheme.colorScheme.error)
                                Text(grandTotalDue.toRupees(),
                                    color      = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Purchases",
                                style = MaterialTheme.typography.bodySmall)
                            Text("${filteredPurchases.size} orders",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // ── Per supplier breakdown ────────────────────────────────
            if (supplierSummaries.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No purchases in this period",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    Text("By supplier",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                items(supplierSummaries) { summary ->
                    SupplierSummaryCard(
                        summary    = summary,
                        onNavigate = {
                            summary.supplier?.let {
                                navController.navigate("supplier_detail/${it.id}")
                            }
                        }
                    )
                }
                item {
                    StockValuationCard()   // stock cost value, selling value, potential profit
                }
                item {
                    // Supplier report navigation card
                    Card(
                        onClick   = { navController.navigate("supplier_report") },
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.LocalShipping, null,
                                    tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text("Supplier report",
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("Spending by supplier & date range",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Supplier summary card ─────────────────────────────────────────────
@Composable
private fun SupplierSummaryCard(
    summary    : SupplierSummary,
    onNavigate : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick   = { expanded = !expanded },
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.supplierName,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("${summary.purchaseCount} purchase${if (summary.purchaseCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(summary.totalSpent.toRupees(),
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium)
                    if (summary.totalDue > 0) {
                        Text("Due: ${summary.totalDue.toRupees()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Paid", style = MaterialTheme.typography.bodySmall)
                    Text(summary.totalPaid.toRupees(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(8.dp))

                // Purchase list for this supplier
                summary.purchases.forEach { purchase ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(purchase.poNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                            Text(formatTimestamp(purchase.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(purchase.grandTotal.toRupees(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (summary.supplier != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick  = onNavigate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View supplier detail →")
                    }
                }
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private data class SupplierSummary(
    val supplier      : Supplier?,
    val supplierName  : String,
    val totalSpent    : Double,
    val totalPaid     : Double,
    val totalDue      : Double,
    val purchaseCount : Int,
    val purchases     : List<Purchase>
)

// ── Date range helper ─────────────────────────────────────────────────
private fun getDateRange(option: String): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    val to  = cal.timeInMillis
    return when (option) {
        "This month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            Pair(cal.timeInMillis, to)
        }
        "Last month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val monthEnd = cal.timeInMillis - 1
            cal.add(Calendar.MONTH, -1)
            Pair(cal.timeInMillis, monthEnd)
        }
        "Last 3 months" -> {
            cal.add(Calendar.MONTH, -3)
            Pair(cal.timeInMillis, to)
        }
        else -> Pair(0L, to) // All time
    }
}

@Composable
fun StockValuationCard(productViewModel: ProductViewModel = viewModel()) {

    LaunchedEffect(Unit) { productViewModel.fetchProducts() }

    val products = productViewModel.productList.value

    // Valuation metrics
    val totalCostValue    = products.sumOf { it.quantity * it.costPrice }
    val totalSellingValue = products.sumOf { it.quantity * it.sellingPrice }
    val potentialProfit   = totalSellingValue - totalCostValue
    val totalItems        = products.size
    val outOfStock        = products.count { it.quantity <= 0 }
    val lowStock          = products.count { it.quantity in 0.001..it.minStockLevel }

    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick   = { expanded = !expanded },
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Coloured top strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .then(
                        Modifier.background(MaterialTheme.colorScheme.tertiary)
                    )
            )
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Inventory, null,
                            tint = MaterialTheme.colorScheme.tertiary)
                        Text("Stock valuation",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            totalCostValue.toRupees(),
                            modifier   = Modifier.padding(
                                horizontal = 10.dp, vertical = 5.dp),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                // Main metrics
                StockValuationRow(
                    label = "Cost value (what you paid)",
                    value = totalCostValue.toRupees(),
                    icon  = Icons.Filled.ShoppingCart,
                    tint  = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                StockValuationRow(
                    label = "Selling value (if all sold)",
                    value = totalSellingValue.toRupees(),
                    icon  = Icons.Filled.TrendingUp,
                    tint  = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(6.dp))
                StockValuationRow(
                    label = "Potential profit",
                    value = potentialProfit.toRupees(),
                    icon  = Icons.Filled.Payments,
                    tint  = MaterialTheme.colorScheme.primary,
                    bold  = true
                )

                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    StockValuationRow(
                        label = "Total products",
                        value = "$totalItems",
                        icon  = Icons.Filled.Inventory,
                        tint  = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    StockValuationRow(
                        label = "Low stock",
                        value = "$lowStock",
                        icon  = Icons.Filled.Warning,
                        tint  = if (lowStock > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    StockValuationRow(
                        label = "Out of stock",
                        value = "$outOfStock",
                        icon  = Icons.Filled.RemoveShoppingCart,
                        tint  = if (outOfStock > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (products.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Top 5 by value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        products
                            .sortedByDescending { it.quantity * it.costPrice }
                            .take(5)
                            .forEach { p ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    Arrangement.SpaceBetween
                                ) {
                                    Text(p.name,
                                        style    = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f))
                                    Text(
                                        "${formatQty(p.quantity)} × ₹${p.costPrice} = ${(p.quantity * p.costPrice).toRupees()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                            }
                    }
                }

                // Expand hint
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockValuationRow(
    label : String,
    value : String,
    icon  : androidx.compose.ui.graphics.vector.ImageVector,
    tint  : androidx.compose.ui.graphics.Color,
    bold  : Boolean = false
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = tint)
            Text(label,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        }
        Text(value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color      = tint)
    }
}