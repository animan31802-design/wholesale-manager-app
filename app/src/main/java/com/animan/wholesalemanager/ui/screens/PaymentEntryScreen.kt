package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    customerId: String,
    navController: NavController
) {
    val viewModel: CustomerViewModel = viewModel()

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchCustomers()
    }

    val customer = viewModel.customerList.value.find { it.id == customerId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record payment") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (customer == null) {
                CircularProgressIndicator()
                return@Column
            }

            // Customer summary card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(customer.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Phone: ${customer.phone}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Outstanding balance: ${customer.balance.toRupees()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (customer.balance > 0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    Text("Total purchase: ${customer.totalPurchase.toRupees()}")
                    Text("Total paid: ${customer.totalPaid.toRupees()}")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; message = "" },
                label = { Text("Payment amount (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional — e.g. Cash, UPI)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (message.isNotEmpty()) {
                Text(message, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick-fill buttons for common amounts
            if (customer.balance > 0) {
                Text("Quick fill:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500, 1000, 2000).forEach { quick ->
                        if (quick <= customer.balance) {
                            OutlinedButton(onClick = { amount = quick.toString() }) {
                                Text("₹$quick")
                            }
                        }
                    }
                    OutlinedButton(onClick = { amount = customer.balance.formatPrice() }) {
                        Text("Full ${customer.balance.toRupees()}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val paid = amount.toDoubleOrNull()
                    when {
                        paid == null || paid <= 0 -> {
                            message = "Enter a valid amount"
                        }
                        paid > customer.balance -> {
                            message = "Amount exceeds outstanding balance (₹${customer.balance})"
                        }
                        else -> {
                            viewModel.recordPayment(customer, paid, note) {
                                message = "Payment of ₹$paid recorded"
                                amount = ""
                                note = ""
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading.value && customer.balance > 0
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Record payment")
                }
            }

            if (customer.balance <= 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No outstanding balance for this customer.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}