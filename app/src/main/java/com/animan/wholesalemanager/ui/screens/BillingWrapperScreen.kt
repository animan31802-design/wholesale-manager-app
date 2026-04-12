package com.animan.wholesalemanager.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.viewmodel.CustomerViewModel

@Composable
fun BillingWrapperScreen(
    customerId: String,
    navController: androidx.navigation.NavController
) {

    val viewModel: CustomerViewModel = viewModel()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchCustomers()
    }

    selectedCustomer = viewModel.customerList.value.find {
        it.id == customerId
    }

    selectedCustomer?.let { customer ->
        BillingScreen(
            customer = customer,
            onBillCreated = {
                viewModel.fetchCustomers()
                navController.popBackStack()
            }
        )
    }
}