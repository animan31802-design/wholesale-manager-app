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
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.utils.LOW_STOCK_THRESHOLD
import com.animan.wholesalemanager.utils.PdfGenerator
import com.animan.wholesalemanager.utils.PdfGenerator.sharePdf
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel

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
    var message by remember { mutableStateOf("") }

    // productId -> quantity in cart (replaces mutableStateOf inside BillItem)
    val cartQty = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        productViewModel.fetchProducts()
    }

    // Derive BillItem list from cartQty map
    val billItems: List<BillItem> by remember {
        derivedStateOf {
            productViewModel.productList.value
                .filter { (cartQty[it.id] ?: 0) > 0 }
                .map { product ->
                    BillItem(
                        productId = product.id,
                        name = product.name,
                        price = product.price,
                        quantity = cartQty[product.id] ?: 1
                    )
                }
        }
    }

    val itemsTotal by remember {
        derivedStateOf { billItems.sumOf { it.price * it.quantity } }
    }

    val finalAmount = itemsTotal + customer.balance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Billing", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Customer: ${customer.name}")
        Text("Previous balance: ₹${customer.balance}")
        Spacer(modifier = Modifier.height(16.dp))

        Text("Products", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(productViewModel.productList.value) { product ->
                val qtyInCart = cartQty[product.id] ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
                            Text(
                                "Stock: ${product.quantity}",
                                color = if (product.quantity <= LOW_STOCK_THRESHOLD)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                if (qtyInCart < product.quantity) {
                                    cartQty[product.id] = qtyInCart + 1
                                }
                            },
                            enabled = product.quantity > qtyInCart
                        ) {
                            Text(if (product.quantity == 0) "Out of stock" else "Add")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Selected items", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
            items(billItems) { item ->
                val product = productViewModel.productList.value.find { it.id == item.productId }
                val availableStock = product?.quantity ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.name)
                            Text("₹${item.price} x ${item.quantity} = ₹${item.price * item.quantity}")
                        }
                        Row {
                            Button(onClick = {
                                val cur = cartQty[item.productId] ?: 1
                                if (cur <= 1) cartQty.remove(item.productId)
                                else cartQty[item.productId] = cur - 1
                            }) { Text("-") }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = {
                                    val cur = cartQty[item.productId] ?: 1
                                    if (cur < availableStock) cartQty[item.productId] = cur + 1
                                },
                                enabled = item.quantity < availableStock
                            ) { Text("+") }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(onClick = { cartQty.remove(item.productId) }) { Text("X") }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Items total: ₹$itemsTotal")
        Text("Previous balance: ₹${customer.balance}")
        Text("Final amount: ₹$finalAmount", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = paidAmount,
            onValueChange = { paidAmount = it },
            label = { Text("Paid amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (billItems.isEmpty()) { message = "Add at least one product"; return@Button }
                val paid = paidAmount.toDoubleOrNull()
                if (paid == null) { message = "Enter a valid paid amount"; return@Button }
                if (paid < 0) { message = "Paid amount cannot be negative"; return@Button }
                if (paid > finalAmount) { message = "Paid amount exceeds total"; return@Button }

                customerViewModel.createBill(
                    customer, billItems, itemsTotal, paid
                ) { billId ->
                    // FIX: pass finalAmount (total owed) not finalAmount - paid.
                    // PrinterManager calculates balance = finalAmount - paidAmount internally.
                    val printResult = printerManager.printBill(
                        context, customer, billItems, itemsTotal, paid, finalAmount
                    )

                    val balance = finalAmount - paid
                    val file = PdfGenerator.generateBillPdf(
                        context, customer, billItems, itemsTotal, paid, balance
                    )
                    sharePdf(context, file)

                    message = "Bill saved. $printResult"
                    cartQty.clear()
                    onBillCreated()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = billItems.isNotEmpty() && !customerViewModel.isLoading.value
        ) {
            if (customerViewModel.isLoading.value) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Save bill")
            }
        }

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }

        customerViewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}