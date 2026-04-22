package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {

    val billViewModel:    BillViewModel    = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val printerManager = remember { PrinterManager() }
    val context        = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        billViewModel.fetchBills()
        expenseViewModel.fetchExpenses()
        productViewModel.fetchLowStockProducts()
    }

    val lowStockCount = productViewModel.lowStockList.value.size

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController  = navController,
                onItemClick    = { scope.launch { drawerState.close() } },
                lowStockCount  = lowStockCount
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // ── Today ─────────────────────────────────────────────
                item {
                    val (todayBilled, todayPaid, todayDue) = billViewModel.getTodaySummary()
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryChip("Sales", todayBilled.toRupees(), Modifier.weight(1f))
                        SummaryChip("Paid",  todayPaid.toRupees(),   Modifier.weight(1f))
                        SummaryChip(
                            label = "Due",
                            value = todayDue.toRupees(),
                            modifier = Modifier.weight(1f),
                            // Only colour red if genuinely non-zero after rounding
                            valueColor = if (todayDue > 0.001)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Overall ───────────────────────────────────────────
                item {
                    val overall       = billViewModel.getOverallSummary()
                    val totalExpenses = expenseViewModel.expenseList.value
                        .sumOf { it.amount }.round2dp()
                    // Net cash = money actually received minus expenses
                    val netCash       = (overall.totalPaid - totalExpenses).round2dp()

                    Text("Overall", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryRow("Total billed",   overall.totalBilled.toRupees())
                            SummaryRow("Total received", overall.totalPaid.toRupees())
                            SummaryRow(
                                label      = "Total due",
                                value      = overall.totalDue.toRupees(),
                                valueColor = if (overall.totalDue > 0.001)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                            SummaryRow(
                                label      = "Total expenses",
                                value      = totalExpenses.toRupees(),
                                valueColor = MaterialTheme.colorScheme.error
                            )
                            HorizontalDivider()
                            SummaryRow(
                                label      = "Net (received − expenses)",
                                value      = netCash.toRupees(),
                                valueColor = if (netCash >= 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // ── Low stock ─────────────────────────────────────────
                if (lowStockCount > 0) {
                    item {
                        Card(
                            onClick = { navController.navigate("product_list") },
                            colors  = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.Warning, null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer)
                                Column {
                                    Text(
                                        "$lowStockCount product${if (lowStockCount > 1) "s" else ""} low in stock",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("Tap to view",
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // ── Quick actions ─────────────────────────────────────
                item {
                    Text("Quick actions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard("New bill",  Icons.Filled.Receipt,
                            { navController.navigate("customer_list") }, Modifier.weight(1f))
                        QuickActionCard("Products",  Icons.Filled.Inventory,
                            { navController.navigate("product_list") },  Modifier.weight(1f))
                        QuickActionCard("Reports",   Icons.Filled.BarChart,
                            { navController.navigate("reports") },       Modifier.weight(1f))
                    }
                }

                // ── Test print ────────────────────────────────────────
                item {
                    var printMessage by remember { mutableStateOf("") }
                    OutlinedButton(
                        onClick = { printMessage = printerManager.printTestBill(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Print, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Test print")
                    }
                    if (printMessage.isNotEmpty()) {
                        Text(printMessage, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label:      String,
    value:      String,
    modifier:   Modifier = Modifier,
    valueColor: Color    = Color.Unspecified
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value,
                style = MaterialTheme.typography.titleMedium,
                color = if (valueColor == Color.Unspecified)
                    MaterialTheme.colorScheme.onSurface else valueColor)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryRow(
    label:      String,
    value:      String,
    valueColor: Color = Color.Unspecified
) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = if (valueColor == Color.Unspecified)
            MaterialTheme.colorScheme.onSurface else valueColor)
    }
}

@Composable
private fun QuickActionCard(
    label:    String,
    icon:     ImageVector,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}