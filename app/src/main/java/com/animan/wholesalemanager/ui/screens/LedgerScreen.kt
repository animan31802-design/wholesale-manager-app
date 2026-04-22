package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerWrapperScreen(customerId: String, navController: NavController) {
    val customerViewModel: CustomerViewModel = viewModel()
    var customer by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(Unit) { customerViewModel.fetchCustomers() }

    LaunchedEffect(customerViewModel.customerList.value) {
        customer = customerViewModel.customerList.value.find { it.id == customerId }
    }

    customer?.let { LedgerScreen(customer = it, navController = navController) }
        ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(customer: Customer, navController: NavController? = null) {
    val viewModel: LedgerViewModel = viewModel()
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(Unit) { viewModel.fetchLedger(customer.id) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ledger — ${customer.name}") },
            navigationIcon = {
                navController?.let {
                    IconButton(onClick = { it.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Customer balance summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (customer.balance > 0)
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total purchase"); Text(customer.totalPurchase.toRupees())
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total paid"); Text(customer.totalPaid.toRupees())
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Balance",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (customer.balance > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(customer.balance.toRupees(),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (customer.balance > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (viewModel.isLoading.value) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (viewModel.ledgerList.value.isEmpty() && !viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items(viewModel.ledgerList.value) { entry ->

                        // Color coding: CREDIT=red(they owe more), PAYMENT=green(they paid),
                        // REFUND=purple(we reversed a bill)
                        val (amountText, amountColor, typeLabel) = when (entry.type) {
                            "CREDIT"  -> Triple("+₹${entry.amount.formatPrice()}", MaterialTheme.colorScheme.error,   "Bill")
                            "PAYMENT" -> Triple("-₹${entry.amount.formatPrice()}", MaterialTheme.colorScheme.primary,  "Payment")
                            "REFUND"  -> Triple("-₹${entry.amount.formatPrice()}", Color(0xFF7C4DFF),                 "Refund")
                            else      -> Triple("₹${entry.amount.formatPrice()}",  MaterialTheme.colorScheme.onSurface,"")
                        }

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(typeLabel, style = MaterialTheme.typography.titleSmall)
                                    if (entry.note.isNotBlank())
                                        Text(entry.note, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dateFormat.format(Date(entry.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(amountText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = amountColor)
                            }
                        }
                    }
                }
            }
        }
    }
}