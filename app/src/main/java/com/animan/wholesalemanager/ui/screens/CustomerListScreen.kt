package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@Composable
fun CustomerListScreen(navController: NavController) {
    val viewModel: CustomerViewModel = viewModel()

    var searchQuery by remember { mutableStateOf("") }
    var deleteTargetId by remember { mutableStateOf<String?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }

    LaunchedEffect(true) { viewModel.fetchCustomers() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) viewModel.fetchCustomers()
        else viewModel.searchCustomers(searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Customers", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name or phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { navController.navigate("add_customer") }) {
            Text("Add customer")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModel.isLoading.value) CircularProgressIndicator()

        LazyColumn {
            items(viewModel.customerList.value) { customer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        Text("Phone: ${customer.phone}")
                        if (customer.address.isNotBlank()) Text("Address: ${customer.address}")
                        Text(
                            "Balance: ₹${customer.balance}",
                            color = if (customer.balance > 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 1: Bill + Ledger + Payment
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { navController.navigate("billing/${customer.id}") }) {
                                Text("Bill")
                            }
                            OutlinedButton(onClick = { navController.navigate("ledger/${customer.id}") }) {
                                Text("Ledger")
                            }
                            OutlinedButton(
                                onClick = { navController.navigate("payment/${customer.id}") },
                                enabled = customer.balance > 0
                            ) {
                                Text("Pay")
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: Edit + Delete
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { navController.navigate("edit_customer/${customer.id}") }
                            ) {
                                Text("Edit")
                            }
                            Button(
                                onClick = {
                                    deleteTargetId = customer.id
                                    deleteTargetName = customer.name
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        viewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    // Confirm delete dialog
    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete customer") },
            text = { Text("Delete $deleteTargetName? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(deleteTargetId!!) {}
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            }
        )
    }
}