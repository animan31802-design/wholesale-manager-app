package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@Composable
fun CustomerListScreen(
    navController: androidx.navigation.NavController
) {

    val viewModel: CustomerViewModel = viewModel()

    LaunchedEffect(true) {
        viewModel.fetchCustomers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Customers",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                navController.navigate("add_customer")
            }
        ) {
            Text("Add Customer")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModel.isLoading.value) {
            CircularProgressIndicator()
        }

        LazyColumn {

            items(viewModel.customerList.value) { customer ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {

                    Column(modifier = Modifier.padding(10.dp)) {

                        Text("Name: ${customer.name}")
                        Text("Phone: ${customer.phone}")
                        Text("Balance: ₹${customer.balance}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {

                            // 🧾 Open Billing
                            Button(
                                onClick = {
                                    navController.navigate("billing/${customer.id}")
                                }
                            ) {
                                Text("Bill")
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // 📊 Open Ledger
                            Button(
                                onClick = {
                                    navController.navigate("ledger/${customer.id}")
                                }
                            ) {
                                Text("Ledger")
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
}