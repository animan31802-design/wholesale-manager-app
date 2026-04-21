package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.ui.components.UpiPaymentDialog
import com.animan.wholesalemanager.utils.PdfGenerator
import com.animan.wholesalemanager.utils.PdfGenerator.sharePdf
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.WhatsAppShare
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.delay

@Composable
fun BillingScreen(customer: Customer, onBillCreated: () -> Unit) {

    val customerViewModel: CustomerViewModel = viewModel()
    val productViewModel: ProductViewModel   = viewModel()
    val printerManager = remember { PrinterManager() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var paidAmount       by remember { mutableStateOf("") }
    var message          by remember { mutableStateOf("") }
    var showCart         by remember { mutableStateOf(false) }
    val cartQty = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(Unit) {
        productViewModel.fetchFrequentProducts()
        productViewModel.fetchCategories()
        productViewModel.fetchProducts()
        delay(200); searchFocusRequester.requestFocus()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) productViewModel.fetchProducts()
        else { delay(300); productViewModel.searchProducts(searchQuery) }
    }

    LaunchedEffect(selectedCategory) {
        if (searchQuery.isBlank()) productViewModel.filterByCategory(selectedCategory)
    }

    // Derive BillItem list with GST
    val billItems: List<BillItem> by remember {
        derivedStateOf {
            productViewModel.productList.value
                .filter { (cartQty[it.id] ?: 0) > 0 }
                .map { p -> BillItem(
                    productId  = p.id, name = p.name, price = p.sellingPrice,
                    costPrice  = p.costPrice, unit = p.unit,
                    quantity   = cartQty[p.id] ?: 1, gstPercent = p.gstPercent
                )}
        }
    }

    val itemsTotal by remember { derivedStateOf { billItems.sumOf { it.price * it.quantity } } }
    val gstTotal   by remember { derivedStateOf { billItems.sumOf { it.gstAmount } } }
    val grandTotal by remember { derivedStateOf { itemsTotal + gstTotal } }
    val totalOwed  = grandTotal + customer.balance

    fun addToCart(p: Product) {
        val cur = cartQty[p.id] ?: 0
        if (cur < p.quantity) cartQty[p.id] = cur + 1
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Top bar
        Surface(shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        if (customer.balance > 0)
                            Text("Prev. balance: ₹${customer.balance.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                    }
                    BadgedBox(badge = {
                        if (cartQty.isNotEmpty()) Badge { Text(cartQty.values.sum().toString()) }
                    }) {
                        Button(onClick = { showCart = !showCart }) {
                            Text(if (showCart) "Products" else "Cart ₹${grandTotal.toInt()}")
                        }
                    }
                }
            }
        }

        if (showCart) {
            CartView(
                billItems = billItems, customer = customer,
                itemsTotal = itemsTotal, gstTotal = gstTotal,
                grandTotal = grandTotal, totalOwed = totalOwed,
                paidAmount = paidAmount,
                onPaidAmountChange = { paidAmount = it; message = "" },
                message = message,
                isLoading = customerViewModel.isLoading.value,
                errorMessage = customerViewModel.errorMessage.value,
                onQtyDecrease = { item ->
                    val cur = cartQty[item.productId] ?: 1
                    if (cur <= 1) cartQty.remove(item.productId) else cartQty[item.productId] = cur - 1
                },
                onQtyIncrease = { item ->
                    val avail = productViewModel.productList.value.find { it.id == item.productId }?.quantity ?: 0
                    val cur = cartQty[item.productId] ?: 1
                    if (cur < avail) cartQty[item.productId] = cur + 1
                },
                onRemove = { cartQty.remove(it.productId) },
                onSaveBill = {
                    if (billItems.isEmpty()) { message = "Add at least one product"; return@CartView }
                    val paid = paidAmount.toDoubleOrNull()
                    if (paid == null)      { message = "Enter a valid paid amount"; return@CartView }
                    if (paid < 0)          { message = "Cannot be negative"; return@CartView }
                    if (paid > totalOwed)  { message = "Exceeds total amount"; return@CartView }

                    customerViewModel.createBill(
                        customer, billItems, itemsTotal, gstTotal, grandTotal, paid
                    ) { _ ->
                        val printResult = printerManager.printBill(
                            context, customer, billItems, itemsTotal, paid, totalOwed
                        )
                        val balanceLeft = totalOwed - paid
                        val file = PdfGenerator.generateBillPdf(
                            context, customer, billItems, itemsTotal, gstTotal, paid, balanceLeft
                        )

                        //sharePdf(context, file)

                        // WhatsApp text summary (no extra tap needed — fires alongside PDF)
                        // Comment this out if you only want PDF share
                        WhatsAppShare.shareTextSummary(context, customer, billItems, grandTotal, paid, balanceLeft)

                        message = "Bill saved. $printResult"
                        cartQty.clear()
                        onBillCreated()
                    }
                }
            )
        } else {
            // Product search view
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; selectedCategory = "All" },
                    label = { Text("Search product, category, barcode…") },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(searchFocusRequester),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty())
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, null)
                            }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        productViewModel.productList.value.firstOrNull()?.let { addToCart(it) }
                        keyboardController?.hide()
                    })
                )

                val categories = listOf("All") + productViewModel.categoryList.value
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat; searchQuery = "" },
                            label = { Text(cat) })
                    }
                }

                if (searchQuery.isBlank() && selectedCategory == "All" &&
                    productViewModel.frequentList.value.isNotEmpty()) {
                    Text("  Frequently sold", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(productViewModel.frequentList.value) { p ->
                            FrequentProductChip(p, cartQty[p.id] ?: 0) { addToCart(p) }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                when {
                    productViewModel.isLoading.value ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        }
                    productViewModel.productList.value.isEmpty() && searchQuery.isNotBlank() ->
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No products found for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    else -> LazyColumn {
                        items(productViewModel.productList.value, key = { it.id }) { p ->
                            ProductBillingRow(p, cartQty[p.id] ?: 0,
                                onAdd = { addToCart(p) },
                                onRemove = {
                                    val cur = cartQty[p.id] ?: 0
                                    if (cur <= 1) cartQty.remove(p.id)
                                    else cartQty[p.id] = cur - 1
                                })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductBillingRow(product: Product, qtyInCart: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    val outOfStock = product.quantity == 0
    val atLimit    = qtyInCart >= product.quantity
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            val gstText = if (product.gstPercent > 0) "  GST ${product.gstPercent.toInt()}%" else ""
            Text("₹${product.sellingPrice.formatPrice()}  |  ${product.unit}  |  Stock: ${product.quantity}$gstText",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.quantity <= product.minStockLevel)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            if (product.category.isNotBlank())
                Text(product.category, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
        }
        if (qtyInCart > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledIconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Text("-") }
                Text("$qtyInCart", style = MaterialTheme.typography.titleSmall)
                FilledIconButton(onClick = onAdd, modifier = Modifier.size(32.dp), enabled = !atLimit) { Text("+") }
            }
        } else {
            Button(onClick = onAdd, enabled = !outOfStock, modifier = Modifier.height(36.dp)) {
                Text(if (outOfStock) "Out of stock" else "Add")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

@Composable
private fun FrequentProductChip(product: Product, qtyInCart: Int, onAdd: () -> Unit) {
    ElevatedCard(onClick = onAdd, enabled = product.quantity > qtyInCart, modifier = Modifier.width(110.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(product.name.take(14), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text("₹${product.sellingPrice.toInt()}", style = MaterialTheme.typography.bodySmall)
            if (qtyInCart > 0) Text("In cart: $qtyInCart",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CartView(
    billItems: List<BillItem>,
    customer: Customer,
    itemsTotal: Double,
    gstTotal: Double,
    grandTotal: Double,
    totalOwed: Double,
    paidAmount: String,
    onPaidAmountChange: (String) -> Unit,
    message: String,
    isLoading: Boolean,
    errorMessage: String?,
    onQtyDecrease: (BillItem) -> Unit,
    onQtyIncrease: (BillItem) -> Unit,
    onRemove: (BillItem) -> Unit,
    onSaveBill: () -> Unit
) {

    var showUpiDialog by remember { mutableStateOf(false) }
    var upiAmount     by remember { mutableStateOf(0.0) }

    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        if (billItems.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Cart is empty. Go back and add products.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@LazyColumn
        }

        items(billItems) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text("₹${item.price.formatPrice()} × ${item.quantity} ${item.unit} = ₹${"%.2f".format(item.price * item.quantity)}",
                            style = MaterialTheme.typography.bodySmall)
                        if (item.gstPercent > 0)
                            Text("GST ${item.gstPercent.toInt()}% = ₹${item.gstAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(onClick = { onQtyDecrease(item) }, Modifier.size(32.dp)) { Text("-") }
                        Text("${item.quantity}")
                        FilledIconButton(onClick = { onQtyIncrease(item) }, Modifier.size(32.dp)) { Text("+") }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }

        // Bill summary card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Items total"); Text("₹${"%.2f".format(itemsTotal)}") }
                    if (gstTotal > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("GST", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${gstTotal.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (customer.balance > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Previous balance", color = MaterialTheme.colorScheme.error)
                            Text("₹${customer.balance.toInt()}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total payable", style = MaterialTheme.typography.titleMedium)
                        Text("₹${totalOwed.toInt()}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = paidAmount, onValueChange = onPaidAmountChange,
                label = { Text("Amount paid (₹)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        }

        item {
            val fa = totalOwed.toInt()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1000, 2000).filter { it <= fa }.forEach { amt ->
                    OutlinedButton(onClick = { onPaidAmountChange(amt.toString()) }) { Text("₹$amt") }
                }
                OutlinedButton(onClick = { onPaidAmountChange(fa.toString()) }) { Text("Full ₹$fa") }
            }
        }

        item {
            // UPI QR Pay button
            OutlinedButton(
                onClick = {
                    upiAmount = totalOwed - (paidAmount.toDoubleOrNull() ?: 0.0)
                    if (upiAmount > 0) showUpiDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pay via UPI QR")
            }
        }

        if (message.isNotEmpty()) item { Text(message, color = MaterialTheme.colorScheme.primary) }
        errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }

        item {
            Button(onClick = onSaveBill, modifier = Modifier.fillMaxWidth(),
                enabled = billItems.isNotEmpty() && !isLoading) {
                if (isLoading) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Save bill")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showUpiDialog) {
        UpiPaymentDialog(
            amount = upiAmount,
            billNote = "Invoice",
            onMarkPaid = {
                // Set paid amount to full remaining balance
                onPaidAmountChange(upiAmount.toInt().toString())
                showUpiDialog = false
            },
            onDismiss = { showUpiDialog = false }
        )
    }
}