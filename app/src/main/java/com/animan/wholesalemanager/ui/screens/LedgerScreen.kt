package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

// Wrapper: resolves customerId → full Customer, then renders LedgerScreen
@Composable
fun LedgerWrapperScreen(customerId: String, navController: NavController) {
    val customerViewModel: CustomerViewModel = viewModel()
    var customer by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(Unit) {
        customerViewModel.fetchCustomers()
    }

    LaunchedEffect(customerViewModel.customerList.value) {
        customer = customerViewModel.customerList.value.find { it.id == customerId }
    }

    customer?.let {
        LedgerScreen(customer = it)
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun LedgerScreen(customer: Customer) {
    val viewModel: LedgerViewModel = viewModel()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.fetchLedger(customer.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // FIX: customer.name is now always available
        Text("Ledger — ${customer.name}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Balance: ₹${customer.balance}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        if (viewModel.isLoading.value) {
            CircularProgressIndicator()
        }

        LazyColumn {
            items(viewModel.ledgerList.value) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                if (entry.type == "CREDIT") "+₹${entry.amount}"
                                else "-₹${entry.amount}",
                                color = if (entry.type == "CREDIT")
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            if (entry.note.isNotBlank()) {
                                Text(entry.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(entry.type, style = MaterialTheme.typography.labelSmall)
                            Text(
                                dateFormat.format(Date(entry.timestamp)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}