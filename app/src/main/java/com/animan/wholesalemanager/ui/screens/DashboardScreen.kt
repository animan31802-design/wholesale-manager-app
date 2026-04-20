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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {

    val billViewModel: BillViewModel = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val printerManager = remember { PrinterManager() }
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                navController = navController,
                onItemClick = { scope.launch { drawerState.close() } },
                lowStockCount = lowStockCount
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
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // ── Today's summary ───────────────────────────────────
                item {
                    val (total, paid, balance) = billViewModel.getTodaySummary()
                    Text("Today", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryChip("Sales", "₹${total.toInt()}", Modifier.weight(1f))
                        SummaryChip("Paid", "₹${paid.toInt()}", Modifier.weight(1f))
                        SummaryChip("Due", "₹${balance.toInt()}", Modifier.weight(1f))
                    }
                }

                // ── Overall summary ───────────────────────────────────
                item {
                    val totalSales    = billViewModel.billList.value.sumOf { it.itemsTotal }
                    val totalExpenses = expenseViewModel.expenseList.value.sumOf { it.amount }
                    val profit        = totalSales - totalExpenses
                    Text("Overall", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SummaryRow("Total sales",    "₹${totalSales.toInt()}")
                            SummaryRow("Total expenses", "₹${totalExpenses.toInt()}")
                            HorizontalDivider()
                            SummaryRow(
                                "Profit",
                                "₹${profit.toInt()}",
                                valueColor = if (profit >= 0)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // ── Low stock warning ─────────────────────────────────
                if (lowStockCount > 0) {
                    item {
                        Card(
                            onClick = { navController.navigate("product_list") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Column {
                                    Text(
                                        "$lowStockCount products low in stock",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "Tap to view",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                            .copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Quick actions ─────────────────────────────────────
                item {
                    Text("Quick actions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard(
                            label = "New bill",
                            icon = Icons.Filled.Receipt,
                            onClick = { navController.navigate("customer_list") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Products",
                            icon = Icons.Filled.Inventory,
                            onClick = { navController.navigate("product_list") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            label = "Reports",
                            icon = Icons.Filled.BarChart,
                            onClick = { navController.navigate("reports") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Test print ────────────────────────────────────────
                item {
                    var printMessage by remember { mutableStateOf("") }
                    OutlinedButton(
                        onClick = { printMessage = printerManager.printTestBill(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test print")
                    }
                    if (printMessage.isNotEmpty()) {
                        Text(
                            printMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = valueColor)
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}