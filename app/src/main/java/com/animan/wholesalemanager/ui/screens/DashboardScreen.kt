package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.AuthViewModel
import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {

    val billViewModel:    BillViewModel    = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val printerManager = remember { PrinterManager() }
    val context        = LocalContext.current
    val scope          = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var printMessage   by remember { mutableStateOf("") }
    var isPrinting     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billViewModel.fetchBills()
        expenseViewModel.fetchExpenses()
        productViewModel.fetchLowStockProducts()
        authViewModel.checkAndRestoreBackup(context) {
            // Restore wrote new data to SQLite — re-fetch so UI updates immediately
            billViewModel.fetchBills()
            expenseViewModel.fetchExpenses()
            productViewModel.fetchLowStockProducts()
        }
    }

    val restoreMsg = authViewModel.restoreStatus.value
    LaunchedEffect(restoreMsg) {
        if (!restoreMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message  = restoreMsg,
                duration = SnackbarDuration.Long
            )
            authViewModel.restoreStatus.value = null   // clear so it doesn't re-show
        }
    }

    val lowStockCount = productViewModel.lowStockList.value.size

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                onItemClick   = { scope.launch { drawerState.close() } },
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // ── TODAY CARD ────────────────────────────────────────
                item {
                    val (todayBilled, todayPaid, todayDue) = billViewModel.getTodaySummary()
                    val hasDue = todayDue > 0.001

                    // Same card structure as CustomerListScreen
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            // Coloured top strip — red if there's due, primary otherwise
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        if (hasDue) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header row — avatar + label
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Today, null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Today",
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("Daily summary",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Three metric badges — same Surface badge style as customer balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricBadge(
                                        label    = "Sales",
                                        value    = todayBilled.toRupees(),
                                        icon     = Icons.Filled.TrendingUp,
                                        color    = MaterialTheme.colorScheme.primary,
                                        bgColor  = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBadge(
                                        label    = "Paid",
                                        value    = todayPaid.toRupees(),
                                        icon     = Icons.Filled.CheckCircle,
                                        color    = MaterialTheme.colorScheme.secondary,
                                        bgColor  = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricBadge(
                                        label    = "Due",
                                        value    = todayDue.toRupees(),
                                        icon     = if (hasDue) Icons.Filled.AccountBalanceWallet
                                        else Icons.Filled.CheckCircle,
                                        color    = if (hasDue) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        bgColor  = if (hasDue) MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── OVERALL CARD ──────────────────────────────────────
                item {
                    val overall       = billViewModel.getOverallSummary()
                    val totalExpenses = expenseViewModel.expenseList.value
                        .sumOf { it.amount }.round2dp()
                    val netCash       = (overall.totalPaid - totalExpenses).round2dp()
                    val netPositive   = netCash >= 0

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        if (netPositive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                                    )
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AccountBalance, null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Overall",
                                            style      = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("All-time summary",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    // Net badge — top right
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (netPositive)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            netCash.toRupees(),
                                            modifier   = Modifier.padding(
                                                horizontal = 10.dp, vertical = 5.dp),
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = if (netPositive)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.height(10.dp))

                                // Rows — same SummaryRow style
                                DashSummaryRow("Total billed",
                                    overall.totalBilled.toRupees(),
                                    Icons.Filled.ReceiptLong,
                                    MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                DashSummaryRow("Total received",
                                    overall.totalPaid.toRupees(),
                                    Icons.Filled.Payments,
                                    MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(6.dp))
                                DashSummaryRow(
                                    label     = "Total due",
                                    value     = overall.totalDue.toRupees(),
                                    icon      = Icons.Filled.AccountBalanceWallet,
                                    iconTint  = if (overall.totalDue > 0.001)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    valueColor = if (overall.totalDue > 0.001)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                DashSummaryRow("Total expenses",
                                    totalExpenses.toRupees(),
                                    Icons.Filled.MoneyOff,
                                    MaterialTheme.colorScheme.error,
                                    valueColor = MaterialTheme.colorScheme.error)

                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.height(8.dp))

                                DashSummaryRow(
                                    label      = "Net (received − expenses)",
                                    value      = netCash.toRupees(),
                                    icon       = if (netPositive) Icons.Filled.TrendingUp
                                    else Icons.Filled.TrendingDown,
                                    iconTint   = if (netPositive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    valueColor = if (netPositive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    bold       = true
                                )
                            }
                        }
                    }
                }

                // ── LOW STOCK CARD ────────────────────────────────────
                if (lowStockCount > 0) {
                    item {
                        Card(
                            onClick   = { navController.navigate("product_list") },
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Warning, null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "$lowStockCount product${if (lowStockCount > 1) "s" else ""} low in stock",
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.error
                                        )
                                        Text("Tap to restock",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Filled.ChevronRight, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // ── QUICK ACTIONS ─────────────────────────────────────
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.GridView, null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("Quick actions",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    QuickActionButton(
                                        icon    = Icons.Filled.Receipt,
                                        label   = "New Bill",
                                        tint    = MaterialTheme.colorScheme.primary,
                                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                                        onClick = { navController.navigate("customer_list") }
                                    )
                                    QuickActionButton(
                                        icon    = Icons.Filled.Inventory,
                                        label   = "Products",
                                        tint    = MaterialTheme.colorScheme.secondary,
                                        bgColor = MaterialTheme.colorScheme.secondaryContainer,
                                        onClick = { navController.navigate("product_list") }
                                    )
                                    QuickActionButton(
                                        icon    = Icons.Filled.BarChart,
                                        label   = "Reports",
                                        tint    = MaterialTheme.colorScheme.tertiary,
                                        bgColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        onClick = { navController.navigate("reports") }
                                    )
                                    QuickActionButton(
                                        icon    = Icons.Filled.PeopleAlt,
                                        label   = "Customers",
                                        tint    = MaterialTheme.colorScheme.primary,
                                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                                        onClick = { navController.navigate("customer_list") }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── TEST PRINT CARD ───────────────────────────────────
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Print, null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Printer",
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold)
                                        Text("Test your printer connection",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant
                                )
                                Spacer(Modifier.height(10.dp))

                                FilledTonalButton(
                                    onClick  = {
                                        if (isPrinting) return@FilledTonalButton
                                        isPrinting = true
                                        printMessage = ""
                                        val appCtx = context.applicationContext
                                        scope.launch(Dispatchers.Default) {
                                            val result = PrinterManager().printTestBill(appCtx)
                                            withContext(Dispatchers.Main) {
                                                isPrinting   = false
                                                printMessage = result
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled  = !isPrinting
                                ) {
                                    if (isPrinting) {
                                        CircularProgressIndicator(
                                            modifier    = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Printing…")
                                    } else {
                                        Icon(Icons.Filled.Print, null,
                                            modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Test print")
                                    }
                                }

                                if (printMessage.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (printMessage.contains("success", ignoreCase = true) ||
                                            printMessage.contains("வெற்றி"))
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                if (printMessage.contains("success", ignoreCase = true) ||
                                                    printMessage.contains("வெற்றி"))
                                                    Icons.Filled.CheckCircle
                                                else Icons.Filled.Error,
                                                null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (printMessage.contains("success", ignoreCase = true) ||
                                                    printMessage.contains("வெற்றி"))
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                printMessage,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (printMessage.contains("success", ignoreCase = true) ||
                                                    printMessage.contains("வெற்றி"))
                                                    MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // Logout backup overlay
            if (authViewModel.isLoggingOut.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier            = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Backing up your data…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Please wait before closing the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Restore overlay
            if (authViewModel.isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape     = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier            = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Restoring your data…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Fetching your last backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Metric badge (used in Today card) ────────────────────────────────

@Composable
private fun MetricBadge(
    label:    String,
    value:    String,
    icon:     ImageVector,
    color:    Color,
    bgColor:  Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Text(value,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = color)
            Text(label,
                fontSize = 10.sp,
                color    = color.copy(alpha = 0.7f))
        }
    }
}

// ── Summary row with icon (used in Overall card) ──────────────────────

@Composable
private fun DashSummaryRow(
    label:      String,
    value:      String,
    icon:       ImageVector,
    iconTint:   Color,
    valueColor: Color = Color.Unspecified,
    bold:       Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = iconTint)
            Text(
                label,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Text(
            value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color      = if (valueColor == Color.Unspecified)
                MaterialTheme.colorScheme.onSurface else valueColor
        )
    }
}

// ── Quick action button (icon + label stacked, same as ListActionButton) ──

@Composable
private fun QuickActionButton(
    icon:    ImageVector,
    label:   String,
    tint:    Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(64.dp)
    ) {
        FilledIconButton(
            onClick  = onClick,
            modifier = Modifier.size(48.dp),
            colors   = IconButtonDefaults.filledIconButtonColors(
                containerColor = bgColor
            )
        ) {
            Icon(icon, contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint     = tint)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize   = 10.sp,
            color      = tint,
            fontWeight = FontWeight.Medium
        )
    }
}