package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.data.local.Customer

@Composable
fun BillHistoryScreen(
    navController: NavController
) {

    val viewModel: BillViewModel = viewModel()
    val printerManager = remember { PrinterManager() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchBills()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Bill History", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModel.isLoading.value) {
            CircularProgressIndicator()
        }

        LazyColumn {

            items(viewModel.billList.value) { bill ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    onClick = {
                        // 🔥 REPRINT
                        printerManager.printBill(
                            context,
                            Customer(
                                id = bill.customerId,
                                name = bill.customerName,
                                balance = bill.balance
                            ),
                            bill.items,
                            bill.itemsTotal,
                            bill.paidAmount,
                            bill.balance
                        )
                    }
                ) {

                    Column(modifier = Modifier.padding(10.dp)) {

                        Text("Customer: ${bill.customerName}")
                        Text("Total: ₹${bill.itemsTotal}")
                        Text("Paid: ₹${bill.paidAmount}")
                        Text("Balance: ₹${bill.balance}")
                    }
                }
            }
        }

        viewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}