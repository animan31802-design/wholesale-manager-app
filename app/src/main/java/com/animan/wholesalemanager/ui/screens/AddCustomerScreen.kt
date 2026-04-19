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
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    navController: NavController,
    customerId: String? = null   // null = add mode, non-null = edit mode
) {
    val viewModel: CustomerViewModel = viewModel()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // In edit mode, pre-fill fields once the customer list is available
    LaunchedEffect(Unit) {
        viewModel.fetchCustomers()
    }

    LaunchedEffect(viewModel.customerList.value) {
        if (!loaded && customerId != null) {
            val customer = viewModel.customerList.value.find { it.id == customerId }
            customer?.let {
                name = it.name
                phone = it.phone
                address = it.address
                loaded = true
            }
        }
    }

    val isEditMode = customerId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit customer" else "Add customer") },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Customer name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (isEditMode) {
                        val existing = viewModel.customerList.value.find { it.id == customerId }
                        existing?.let {
                            viewModel.updateCustomer(
                                it.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    address = address.trim()
                                )
                            ) { navController.popBackStack() }
                        }
                    } else {
                        viewModel.addCustomer(name, phone, address) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (isEditMode) "Update customer" else "Save customer")
                }
            }
        }
    }
}