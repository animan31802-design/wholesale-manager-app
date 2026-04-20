package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.viewmodel.BillViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillHistoryScreen(navController: NavController) {
    val viewModel: BillViewModel = viewModel()
    val printerManager = remember { PrinterManager() }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var searchQuery   by remember { mutableStateOf("") }
    var printMessage  by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.fetchBills() }

    val filteredBills = remember(viewModel.billList.value, searchQuery) {
        if (searchQuery.isBlank()) viewModel.billList.value
        else viewModel.billList.value.filter {
            it.customerName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill history") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by customer name…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            if (printMessage.isNotEmpty()) {
                Text(
                    printMessage,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (viewModel.isLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (filteredBills.isEmpty() && !viewModel.isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (searchQuery.isBlank()) "No bills yet"
                        else "No bills found for \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    items(filteredBills, key = { it.id }) { bill ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        bill.customerName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        dateFormat.format(Date(bill.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Total: ₹${bill.itemsTotal.toInt()}",
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("Paid: ₹${bill.paidAmount.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    if (bill.balance > 0) {
                                        Text("Due: ₹${bill.balance.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                    }
                                }

                                // Items summary (first 2 items)
                                if (bill.items.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val preview = bill.items.take(2)
                                        .joinToString(", ") { "${it.name} ×${it.quantity}" }
                                    val more = if (bill.items.size > 2)
                                        " +${bill.items.size - 2} more" else ""
                                    Text(
                                        preview + more,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val result = printerManager.printBill(
                                                context,
                                                Customer(
                                                    id = bill.customerId,
                                                    name = bill.customerName
                                                ),
                                                bill.items,
                                                bill.itemsTotal,
                                                bill.paidAmount,
                                                bill.itemsTotal + bill.balance
                                            )
                                            printMessage = result
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Filled.Print,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reprint")
                                    }

                                    if (bill.balance > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                navController.navigate("payment/${bill.customerId}")
                                            },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) { Text("Collect payment") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }
}