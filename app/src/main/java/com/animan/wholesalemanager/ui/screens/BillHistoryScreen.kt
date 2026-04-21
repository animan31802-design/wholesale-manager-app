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
import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.utils.WhatsAppShare
import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillHistoryScreen(navController: NavController) {
    val billViewModel     = viewModel<BillViewModel>()
    val customerViewModel = viewModel<CustomerViewModel>()
    val printerManager    = remember { PrinterManager() }
    val context           = LocalContext.current
    val dateFormat        = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    var searchQuery  by remember { mutableStateOf("") }
    var printMessage by remember { mutableStateOf("") }
    var refundTarget by remember { mutableStateOf<Bill?>(null) }

    LaunchedEffect(Unit) {
        billViewModel.fetchBills()
        customerViewModel.fetchCustomers()
    }

    val filteredBills = remember(billViewModel.billList.value, searchQuery) {
        if (searchQuery.isBlank()) billViewModel.billList.value
        else billViewModel.billList.value.filter {
            it.customerName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Bill history") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Search by customer name…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, null) }
                })

            if (printMessage.isNotEmpty())
                Text(printMessage, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall)

            if (billViewModel.isLoading.value) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (filteredBills.isEmpty() && !billViewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isBlank()) "No bills yet" else "No results",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    items(filteredBills, key = { it.id }) { bill ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (bill.isRefunded)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            )) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(bill.customerName, style = MaterialTheme.typography.titleSmall)
                                        if (bill.isRefunded)
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text("Refunded")
                                            }
                                    }
                                    Text(dateFormat.format(Date(bill.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Total: ₹${bill.grandTotal.toInt()}",
                                        style = MaterialTheme.typography.bodySmall)
                                    if (bill.gstTotal > 0)
                                        Text("GST: ₹${bill.gstTotal.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Paid: ₹${bill.paidAmount.toInt()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    if (bill.balance > 0)
                                        Text("Due: ₹${bill.balance.toInt()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error)
                                }

                                if (bill.items.isNotEmpty()) {
                                    val preview = bill.items.take(2)
                                        .joinToString(", ") { "${it.name} ×${it.quantity}" }
                                    val more = if (bill.items.size > 2)
                                        " +${bill.items.size - 2} more" else ""
                                    Text(preview + more, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!bill.isRefunded) {
                                        OutlinedButton(onClick = {
                                            printMessage = printerManager.printBill(
                                                context,
                                                Customer(bill.customerId, bill.customerName),
                                                bill.items, bill.itemsTotal, bill.paidAmount,
                                                bill.grandTotal + bill.balance)
                                        }, modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)) {
                                            Icon(Icons.Filled.Print, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Reprint")
                                        }

                                        if (bill.balance > 0) {
                                            OutlinedButton(onClick = {
                                                navController.navigate("payment/${bill.customerId}")
                                            }, modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp)) {
                                                Text("Collect")
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { refundTarget = bill },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error)
                                        ) { Text("Refund") }

                                        OutlinedButton(
                                            onClick = {
                                                WhatsAppShare.shareTextSummary(
                                                    context,
                                                    Customer(bill.customerId, bill.customerName),
                                                    bill.items, bill.grandTotal,
                                                    bill.paidAmount, bill.balance)
                                            },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) { Text("WhatsApp") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    refundTarget?.let { bill ->
        AlertDialog(
            onDismissRequest = { refundTarget = null },
            title = { Text("Refund bill") },
            text = {
                Text("Refund bill for ${bill.customerName}?\n\n" +
                        "• ₹${bill.grandTotal.toInt()} will be reversed\n" +
                        "• All items will be restocked\n" +
                        "• Customer balance will be adjusted\n\n" +
                        "This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customer = customerViewModel.customerList.value
                            .find { it.id == bill.customerId }
                        customer?.let {
                            customerViewModel.refundBill(bill, it) {
                                refundTarget = null
                                // FIX 1: refresh bill list immediately so UI updates in place
                                billViewModel.fetchBills()
                            }
                        } ?: run { refundTarget = null }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yes, refund") }
            },
            dismissButton = {
                OutlinedButton(onClick = { refundTarget = null }) { Text("Cancel") }
            }
        )
    }
}