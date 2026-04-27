package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Purchase
import com.animan.wholesalemanager.data.local.Supplier
import com.animan.wholesalemanager.data.local.SupplierLedger
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.SupplierViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Purchase history screen ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseHistoryScreen(navController: NavController) {

    val viewModel: SupplierViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchAllPurchases()
        viewModel.fetchSuppliers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Purchase history") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading.value ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            viewModel.purchaseList.value.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inventory, null,
                            modifier = Modifier.size(48.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No purchases yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            else -> LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(viewModel.purchaseList.value, key = { it.id }) { purchase ->
                    val supplier = viewModel.supplierList.value
                        .find { it.id == purchase.supplierId }
                    PurchaseCard(
                        purchase = purchase,
                        onPayRemaining = if (purchase.balance > 0.001 && supplier != null) {
                            { amount, note ->
                                viewModel.recordPayment(supplier, amount, note) {
                                    viewModel.fetchAllPurchases()
                                    // Update purchase balance in DB
                                    viewModel.markPurchasePaid(purchase.id, amount)
                                }
                            }
                        } else null
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ── Supplier detail screen ────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailScreen(supplierId: String, navController: NavController) {

    val viewModel: SupplierViewModel = viewModel()
    var supplier          by remember { mutableStateOf<Supplier?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedTab       by remember { mutableStateOf(0) }
    val tabs = listOf("Ledger", "Purchases")

    LaunchedEffect(Unit) {
        viewModel.fetchSuppliers()
        viewModel.fetchSupplierLedger(supplierId)
        viewModel.fetchPurchasesBySupplier(supplierId)
    }

    LaunchedEffect(viewModel.supplierList.value) {
        supplier = viewModel.supplierList.value.find { it.id == supplierId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(supplier?.name ?: "Supplier") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("purchase/$supplierId")
                    }) {
                        Icon(Icons.Filled.ShoppingCart,
                            contentDescription = "New purchase")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Balance card ──────────────────────────────────────────
            supplier?.let { sup ->
                val hasBalance = sup.balance > 0.001
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBalance)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (hasBalance) "Amount owed to supplier"
                                else "No outstanding balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasBalance) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                sup.balance.toRupees(),
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color      = if (hasBalance) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                        if (hasBalance) {
                            FilledTonalButton(onClick = { showPaymentDialog = true }) {
                                Icon(Icons.Filled.Payments, null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Record payment")
                            }
                        }
                    }
                }
            }

            // ── Tabs ──────────────────────────────────────────────────
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick  = { selectedTab = index },
                        text     = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LedgerTab(viewModel.ledgerList.value)
                1 -> PurchasesTab(
                    purchases  = viewModel.purchaseList.value,
                    supplier   = supplier,
                    onPayRemaining = { purchase, amount, note ->
                        supplier?.let { sup ->
                            viewModel.recordPayment(sup, amount, note) {
                                viewModel.fetchSuppliers()
                                viewModel.fetchSupplierLedger(supplierId)
                                viewModel.fetchPurchasesBySupplier(supplierId)
                                viewModel.markPurchasePaid(purchase.id, amount)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showPaymentDialog) {
        supplier?.let { sup ->
            SupplierPaymentDialog(
                supplier  = sup,
                onConfirm = { amount, note ->
                    viewModel.recordPayment(sup, amount, note) {
                        showPaymentDialog = false
                        viewModel.fetchSuppliers()
                        viewModel.fetchSupplierLedger(supplierId)
                    }
                },
                onDismiss = { showPaymentDialog = false }
            )
        }
    }
}

// ── Ledger tab ────────────────────────────────────────────────────────
@Composable
private fun LedgerTab(entries: List<SupplierLedger>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No ledger entries",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(entries) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (entry.type == "payment") Icons.Filled.Payments
                            else Icons.Filled.ShoppingCart,
                            null,
                            tint     = if (entry.type == "payment")
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                if (entry.type == "payment") "Payment made" else "Purchase",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            if (entry.note.isNotBlank())
                                Text(entry.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        if (entry.type == "payment") "-${entry.amount.toRupees()}"
                        else "+${entry.amount.toRupees()}",
                        fontWeight = FontWeight.Bold,
                        color      = if (entry.type == "payment")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Purchases tab ─────────────────────────────────────────────────────
@Composable
private fun PurchasesTab(
    purchases      : List<Purchase>,
    supplier       : Supplier?,
    onPayRemaining : (Purchase, Double, String) -> Unit
) {
    if (purchases.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No purchases yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(purchases) { purchase ->
            PurchaseCard(
                purchase       = purchase,
                onPayRemaining = if (purchase.balance > 0.001 && supplier != null) {
                    { amount, note -> onPayRemaining(purchase, amount, note) }
                } else null
            )
        }
    }
}

// ── Purchase card with Pay remaining button ───────────────────────────
@Composable
fun PurchaseCard(
    purchase       : Purchase,
    onPayRemaining : ((amount: Double, note: String) -> Unit)? = null
) {
    var expanded         by remember { mutableStateOf(false) }
    var showPayDialog    by remember { mutableStateOf(false) }
    val hasDue           = purchase.balance > 0.001

    Card(
        onClick   = { expanded = !expanded },
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Header row ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(purchase.poNumber,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(purchase.supplierName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTimestamp(purchase.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(purchase.grandTotal.toRupees(),
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium)
                    if (hasDue) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                "Due: ${purchase.balance.toRupees()}",
                                modifier   = Modifier.padding(
                                    horizontal = 6.dp, vertical = 2.dp),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "✓ Paid",
                                modifier   = Modifier.padding(
                                    horizontal = 6.dp, vertical = 2.dp),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Pay remaining button — always visible if due ──────────
            if (hasDue && onPayRemaining != null) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick  = { showPayDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Filled.Payments, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Pay remaining ${purchase.balance.toRupees()}",
                        color = MaterialTheme.colorScheme.error)
                }
            }

            // ── Expanded items breakdown ──────────────────────────────
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                purchase.items.forEach { item ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name,
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium)
                            Text("${formatQty(item.quantity)} ${item.unit} " +
                                    "× ₹${item.costPrice.formatPrice()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.lineTotal.toRupees(),
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                HorizontalDivider()
                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Paid", style = MaterialTheme.typography.bodySmall)
                    Text(purchase.paidAmount.toRupees(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (purchase.gstTotal > 0) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("GST", style = MaterialTheme.typography.bodySmall)
                        Text(purchase.gstTotal.toRupees(),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (purchase.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Note: ${purchase.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Expand hint ───────────────────────────────────────────
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

    // ── Pay remaining dialog ──────────────────────────────────────────
    if (showPayDialog && onPayRemaining != null) {
        PayRemainingDialog(
            balance   = purchase.balance,
            onConfirm = { amount, note ->
                onPayRemaining(amount, note)
                showPayDialog = false
            },
            onDismiss = { showPayDialog = false }
        )
    }
}

// ── Pay remaining dialog ──────────────────────────────────────────────
@Composable
private fun PayRemainingDialog(
    balance   : Double,
    onConfirm : (amount: Double, note: String) -> Unit,
    onDismiss : () -> Unit
) {
    var amount by remember { mutableStateOf(balance.formatPrice()) }
    var note   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay remaining balance") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Outstanding: ${balance.toRupees()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) amount = v
                    },
                    label          = { Text("Amount (₹)") },
                    modifier       = Modifier.fillMaxWidth(),
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix         = { Text("₹") }
                )
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, note.trim())
                },
                enabled  = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Record payment") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Supplier payment dialog ───────────────────────────────────────────
@Composable
fun SupplierPaymentDialog(
    supplier  : Supplier,
    onConfirm : (amount: Double, note: String) -> Unit,
    onDismiss : () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note   by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment to ${supplier.name}") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Outstanding: ${supplier.balance.toRupees()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value         = amount,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) amount = v
                    },
                    label          = { Text("Amount paid (₹)") },
                    modifier       = Modifier.fillMaxWidth(),
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix         = { Text("₹") }
                )
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, note.trim())
                },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Record payment") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────
fun formatTimestamp(ts: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ts))
