package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.ConsumptionItem
import com.animan.wholesalemanager.data.local.Expense
import com.animan.wholesalemanager.utils.MoneyUtils.roundMoney
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockConsumptionScreen(navController: NavController) {
    val productViewModel: ProductViewModel = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // consumedItems: productId → ConsumptionItem
    val consumedItems = remember { mutableStateMapOf<String, ConsumptionItem>() }
    var reason     by remember { mutableStateOf("Internal use") }
    var searchQuery by remember { mutableStateOf("") }
    var message     by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { productViewModel.fetchProducts() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) productViewModel.fetchProducts()
        else productViewModel.searchProducts(searchQuery)
    }

    val totalCost = remember(consumedItems.values.toList()) {
        consumedItems.values.sumOf { it.totalCost }.roundMoney()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Stock consumption") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Search ────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Search product…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }
            )

            // ── Summary bar ───────────────────────────────────────────
            if (consumedItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${consumedItems.size} product(s) selected",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Total cost: ₹${totalCost.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Button(onClick = { showConfirm = true }) {
                            Text("Record")
                        }
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f)
            ) {
                // ── Selected items at top ─────────────────────────────
                if (consumedItems.isNotEmpty()) {
                    item {
                        Text("Selected items", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }

                    items(consumedItems.values.toList(), key = { it.productId }) { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    Text("Cost: ₹${item.costPrice} × ${item.quantity} = ₹${item.totalCost.toInt()}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledIconButton(onClick = {
                                        val cur = consumedItems[item.productId]?.quantity ?: 1
                                        if (cur <= 1) consumedItems.remove(item.productId)
                                        else consumedItems[item.productId] = item.copy(quantity = cur - 1)
                                    }, modifier = Modifier.size(28.dp)) { Text("-") }

                                    Text("${item.quantity}")

                                    val product = productViewModel.productList.value
                                        .find { it.id == item.productId }
                                    val maxQty = product?.quantity ?: item.quantity

                                    FilledIconButton(onClick = {
                                        if (item.quantity < maxQty)
                                            consumedItems[item.productId] = item.copy(quantity = item.quantity + 1)
                                    }, modifier = Modifier.size(28.dp),
                                        enabled = item.quantity < maxQty) { Text("+") }

                                    IconButton(onClick = { consumedItems.remove(item.productId) },
                                        modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = reason, onValueChange = { reason = it },
                            label = { Text("Reason (e.g. Internal use, Damaged, Samples)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            singleLine = true
                        )
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                    item {
                        Text("Add more products", style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                // ── Product list ──────────────────────────────────────
                items(productViewModel.productList.value
                    .filter { it.quantity > 0 && !consumedItems.containsKey(it.id) },
                    key = { it.id }) { product ->

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            Text("Cost: ₹${product.costPrice}  |  Stock: ${product.quantity} ${product.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = {
                            consumedItems[product.id] = ConsumptionItem(
                                productId = product.id,
                                name      = product.name,
                                costPrice = product.costPrice,
                                unit      = product.unit,
                                quantity  = 1
                            )
                        }, modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text("Add")
                        }
                    }
                    HorizontalDivider()
                }
            }

            if (message.isNotEmpty()) {
                Text(message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    // ── Confirm dialog ────────────────────────────────────────────────
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm consumption") },
            text = {
                Column {
                    Text("This will:")
                    Spacer(Modifier.height(8.dp))
                    Text("• Reduce stock for ${consumedItems.size} product(s)")
                    Text("• Record ₹${totalCost.toInt()} as an expense ($reason)")
                    Text("• This affects profit calculations")
                    Spacer(Modifier.height(8.dp))
                    Text("Date: ${dateFormat.format(Date())}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showConfirm = false

                    // Reduce stock for each item
                    consumedItems.values.forEach { item ->
                        productViewModel.restockProduct(
                            productId = item.productId,
                            qty = -item.quantity,   // negative = deduct
                            onSuccess = {}
                        )
                    }

                    // Record as expense so it flows into profit calculation
                    expenseViewModel.addExpense(
                        title  = "Stock consumption — $reason (${consumedItems.size} item(s))",
                        amount = totalCost
                    ) {
                        message = "Recorded. Stock reduced. ₹${totalCost.toInt()} added as expense."
                        consumedItems.clear()
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}