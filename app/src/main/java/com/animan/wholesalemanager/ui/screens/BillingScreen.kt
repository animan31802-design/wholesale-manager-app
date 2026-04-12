package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import com.animan.wholesalemanager.printer.PrinterManager

@Composable
fun BillingScreen(
    customer: Customer,
    onBillCreated: () -> Unit
) {

    val customerViewModel: CustomerViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val printerManager = remember { PrinterManager() }

    val context = LocalContext.current

    var paidAmount by remember { mutableStateOf("") }

    val billItems = remember { mutableStateListOf<BillItem>() }

    LaunchedEffect(Unit) {
        productViewModel.fetchProducts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Billing", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(10.dp))

        Text("Customer: ${customer.name}")
        Text("Previous Balance: ₹${customer.balance}")

        Spacer(modifier = Modifier.height(20.dp))

        // Product List
        Text("Products", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.height(200.dp) // limit height
        ) {

            items(productViewModel.productList.value) { product ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {
                            Text(product.name)
                            Text("₹${product.price}")
                            Text("Stock: ${product.quantity}")
                        }

                        val existing = billItems.find { it.productId == product.id }
                        val currentQtyInCart = existing?.quantity ?: 0

                        Button(
                            onClick = {

                                if (product.quantity <= currentQtyInCart) {
                                    // Out of stock
                                    return@Button
                                }

                                if (existing != null) {
                                    existing.quantity++
                                } else {
                                    billItems.add(
                                        BillItem(
                                            productId = product.id,
                                            name = product.name,
                                            price = product.price,
                                            initialQuantity = 1
                                        )
                                    )
                                }
                            },
                            enabled = product.quantity > currentQtyInCart
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Items List
        Text("Selected Items", style = MaterialTheme.typography.titleMedium)

        LazyColumn {

            items(billItems) { item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Column {
                            Text(item.name)
                            Text("₹${item.price} x ${item.quantity}")
                        }

                        Row {

                            // Decrease
                            Button(onClick = {
                                if (item.quantity > 1) {
                                    item.quantity--
                                }else {
                                    billItems.remove(item)
                                }
                            }) {
                                Text("-")
                            }

                            Spacer(modifier = Modifier.width(5.dp))

                            val product = productViewModel.productList.value
                                .find { it.id == item.productId }

                            val availableStock = product?.quantity ?: 0

                            // Increase
                            Button(
                                onClick = {
                                    if (item.quantity < availableStock) {
                                        item.quantity++
                                    } else {
                                        println("Stock limit reached")
                                    }
                                },
                                enabled = item.quantity < availableStock
                            ) {
                                Text("+")
                            }

                            Spacer(modifier = Modifier.width(5.dp))

                            // Remove
                            Button(onClick = {
                                billItems.remove(item)
                            }) {
                                Text("X")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Calculate total
        val itemsTotal by remember {
            derivedStateOf {
                billItems.sumOf { it.price * it.quantity }
            }
        }

        val finalAmount = itemsTotal + customer.balance

        Text("Items Total: ₹$itemsTotal")
        Text("Final Amount: ₹$finalAmount")

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = paidAmount,
            onValueChange = { paidAmount = it },
            label = { Text("Paid Amount") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        var message by remember { mutableStateOf("") }

        Button(
            onClick = {
                val paid = paidAmount.toDoubleOrNull() ?: 0.0

                customerViewModel.createBill(
                    customer,
                    billItems,
                    itemsTotal,
                    paid
                ) {
                    // Print after saving
                    val result = printerManager.printBill(
                        context,
                        customer,
                        billItems,
                        itemsTotal,
                        paid,
                        finalAmount
                    )

                    message = "Bill saved. $result"

                    onBillCreated()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = billItems.isNotEmpty()
        ) {
            Text("Save Bill")
        }

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}