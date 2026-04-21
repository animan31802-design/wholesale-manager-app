package com.animan.wholesalemanager.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

// FIX 4: Central money formatting — rounds Double to 2dp and removes noise
private fun Double.fmt(): String = "₹${"%.2f".format(this).trimEnd('0').trimEnd('.')}"
private fun Double.fmtInt(): String = "₹${Math.round(this)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(navController: NavController) {
    val viewModel: CustomerViewModel = viewModel()
    val context = LocalContext.current

    var searchQuery      by remember { mutableStateOf("") }
    var deleteTargetId   by remember { mutableStateOf<String?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }

    LaunchedEffect(true) { viewModel.fetchCustomers() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) viewModel.fetchCustomers()
        else viewModel.searchCustomers(searchQuery)
    }

    // FIX 3: Scaffold gives this screen the themed background surface
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers") },
                actions = {
                    IconButton(onClick = { navController.navigate("add_customer") }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add customer")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null)
                        }
                }
            )

            if (viewModel.isLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (viewModel.customerList.value.isEmpty() && !viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No customers yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { navController.navigate("add_customer") }) {
                            Text("Add first customer")
                        }
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    items(viewModel.customerList.value, key = { it.id }) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name,
                                            style = MaterialTheme.typography.titleMedium)
                                        if (customer.phone.isNotBlank()) {
                                            Text("📞 ${customer.phone}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (customer.address.isNotBlank()) {
                                            Text(customer.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    // FIX 5: Call button — only shown when phone is present
                                    if (customer.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${customer.phone}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Call,
                                                contentDescription = "Call ${customer.name}",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // FIX 4: balance displayed as rounded integer — no floating noise
                                Text(
                                    "Balance: ${customer.balance.fmtInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (customer.balance > 0.005)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { navController.navigate("billing/${customer.id}") }) {
                                        Text("Bill")
                                    }
                                    OutlinedButton(onClick = { navController.navigate("ledger/${customer.id}") }) {
                                        Text("Ledger")
                                    }
                                    OutlinedButton(
                                        onClick = { navController.navigate("payment/${customer.id}") },
                                        enabled = customer.balance > 0.005
                                    ) {
                                        Text("Pay")
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { navController.navigate("edit_customer/${customer.id}") }
                                    ) { Text("Edit") }

                                    Button(
                                        onClick = {
                                            deleteTargetId   = customer.id
                                            deleteTargetName = customer.name
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete") }
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

    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete customer") },
            text = { Text("Delete $deleteTargetName? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCustomer(deleteTargetId!!) {}; deleteTargetId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            }
        )
    }
}