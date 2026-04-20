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
import com.animan.wholesalemanager.utils.PdfGenerator
import com.animan.wholesalemanager.utils.PdfGenerator.sharePdf
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.delay

@Composable
fun BillingScreen(
    customer: Customer,
    onBillCreated: () -> Unit
) {
    val customerViewModel: CustomerViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val printerManager = remember { PrinterManager() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // ── State ─────────────────────────────────────────────────────────
    var searchQuery       by remember { mutableStateOf("") }
    var selectedCategory  by remember { mutableStateOf("All") }
    var paidAmount        by remember { mutableStateOf("") }
    var message           by remember { mutableStateOf("") }
    var showCart          by remember { mutableStateOf(false) }

    // productId -> quantity in cart
    val cartQty = remember { mutableStateMapOf<String, Int>() }

    // ── Load data ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        productViewModel.fetchFrequentProducts()
        productViewModel.fetchCategories()
        productViewModel.fetchProducts()
        // Auto-focus search on screen open
        delay(200)
        searchFocusRequester.requestFocus()
    }

    // ── Debounced search (300ms) ──────────────────────────────────────
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            productViewModel.fetchProducts()
        } else {
            delay(300)   // debounce
            productViewModel.searchProducts(searchQuery)
        }
    }

    // ── Category filter ───────────────────────────────────────────────
    LaunchedEffect(selectedCategory) {
        if (searchQuery.isBlank()) {
            productViewModel.filterByCategory(selectedCategory)
        }
    }

    // ── Derived totals ────────────────────────────────────────────────
    val billItems: List<BillItem> by remember {
        derivedStateOf {
            productViewModel.productList.value
                .filter { (cartQty[it.id] ?: 0) > 0 }
                .map { p ->
                    BillItem(
                        productId = p.id,
                        name      = p.name,
                        price     = p.sellingPrice,
                        costPrice = p.costPrice,
                        unit      = p.unit,
                        quantity  = cartQty[p.id] ?: 1
                    )
                }
        }
    }

    val itemsTotal by remember {
        derivedStateOf { billItems.sumOf { it.price * it.quantity } }
    }
    val finalAmount = itemsTotal + customer.balance

    // ── Helper: add one unit of a product to cart ─────────────────────
    fun addToCart(product: Product) {
        val current = cartQty[product.id] ?: 0
        if (current < product.quantity) {
            cartQty[product.id] = current + 1
        }
    }

    // ── UI ────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Top bar area: customer info + cart toggle
        Surface(shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                        if (customer.balance > 0) {
                            Text(
                                "Prev. balance: ₹${customer.balance}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Cart toggle badge
                    BadgedBox(
                        badge = {
                            if (cartQty.isNotEmpty()) {
                                Badge { Text(cartQty.values.sum().toString()) }
                            }
                        }
                    ) {
                        Button(onClick = { showCart = !showCart }) {
                            Text(if (showCart) "Products" else "Cart ₹${itemsTotal.toInt()}")
                        }
                    }
                }
            }
        }

        if (showCart) {
            // ── CART VIEW ──────────────────────────────────────────────
            CartView(
                billItems = billItems,
                customer = customer,
                itemsTotal = itemsTotal,
                finalAmount = finalAmount,
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
                    val avail = productViewModel.productList.value
                        .find { it.id == item.productId }?.quantity ?: 0
                    val cur = cartQty[item.productId] ?: 1
                    if (cur < avail) cartQty[item.productId] = cur + 1
                },
                onRemove = { item -> cartQty.remove(item.productId) },
                onSaveBill = {
                    if (billItems.isEmpty()) { message = "Add at least one product"; return@CartView }
                    val paid = paidAmount.toDoubleOrNull()
                    if (paid == null)   { message = "Enter a valid paid amount"; return@CartView }
                    if (paid < 0)       { message = "Paid amount cannot be negative"; return@CartView }
                    if (paid > finalAmount) { message = "Paid amount exceeds total"; return@CartView }

                    customerViewModel.createBill(customer, billItems, itemsTotal, paid) { _ ->
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
                }
            )
        } else {
            // ── PRODUCT SEARCH VIEW ────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; selectedCategory = "All" },
                    label = { Text("Search product, category, barcode…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(searchFocusRequester),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        // Enter key → add first result immediately
                        productViewModel.productList.value.firstOrNull()?.let { addToCart(it) }
                        keyboardController?.hide()
                    })
                )

                // Category filter chips
                val categories = listOf("All") + productViewModel.categoryList.value
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat; searchQuery = "" },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Frequently sold section (only when not searching)
                if (searchQuery.isBlank() && selectedCategory == "All" &&
                    productViewModel.frequentList.value.isNotEmpty()
                ) {
                    Text(
                        "  Frequently sold",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(productViewModel.frequentList.value) { product ->
                            FrequentProductChip(
                                product = product,
                                qtyInCart = cartQty[product.id] ?: 0,
                                onAdd = { addToCart(product) }
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }

                // Product list
                if (productViewModel.isLoading.value) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                } else if (productViewModel.productList.value.isEmpty() && searchQuery.isNotBlank()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No products found for \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(
                            productViewModel.productList.value,
                            key = { it.id }
                        ) { product ->
                            ProductBillingRow(
                                product = product,
                                qtyInCart = cartQty[product.id] ?: 0,
                                onAdd = { addToCart(product) },
                                onRemove = {
                                    val cur = cartQty[product.id] ?: 0
                                    if (cur <= 1) cartQty.remove(product.id)
                                    else cartQty[product.id] = cur - 1
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Product row in search list ────────────────────────────────────────────────
@Composable
private fun ProductBillingRow(
    product: Product,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val outOfStock = product.quantity == 0
    val atLimit    = qtyInCart >= product.quantity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "₹${product.sellingPrice}  |  ${product.unit}  |  Stock: ${product.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.quantity <= product.minStockLevel)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (product.category.isNotBlank()) {
                Text(
                    product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Qty control
        if (qtyInCart > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Text("-")
                }
                Text("$qtyInCart", style = MaterialTheme.typography.titleSmall)
                FilledIconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp),
                    enabled = !atLimit
                ) {
                    Text("+")
                }
            }
        } else {
            Button(
                onClick = onAdd,
                enabled = !outOfStock,
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (outOfStock) "Out of stock" else "Add")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

// ── Frequently sold chip ──────────────────────────────────────────────────────
@Composable
private fun FrequentProductChip(
    product: Product,
    qtyInCart: Int,
    onAdd: () -> Unit
) {
    ElevatedCard(
        onClick = onAdd,
        enabled = product.quantity > qtyInCart,
        modifier = Modifier.width(110.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                product.name.take(14),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            Text("₹${product.sellingPrice.toInt()}", style = MaterialTheme.typography.bodySmall)
            if (qtyInCart > 0) {
                Text("In cart: $qtyInCart",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Cart view ─────────────────────────────────────────────────────────────────
@Composable
private fun CartView(
    billItems: List<BillItem>,
    customer: Customer,
    itemsTotal: Double,
    finalAmount: Double,
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (billItems.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cart is empty. Go back and add products.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@LazyColumn
        }

        items(billItems) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "₹${item.price} × ${item.quantity} ${item.unit} = ₹${(item.price * item.quantity).toInt()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

        // Bill summary
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Items total")
                        Text("₹${itemsTotal.toInt()}")
                    }
                    if (customer.balance > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Previous balance", color = MaterialTheme.colorScheme.error)
                            Text("₹${customer.balance.toInt()}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Final amount", style = MaterialTheme.typography.titleMedium)
                        Text("₹${finalAmount.toInt()}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // Payment input
        item {
            OutlinedTextField(
                value = paidAmount,
                onValueChange = onPaidAmountChange,
                label = { Text("Amount paid (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        // Quick-fill payment buttons
        item {
            val fa = finalAmount.toInt()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1000, 2000).filter { it <= fa }.forEach { amt ->
                    OutlinedButton(onClick = { onPaidAmountChange(amt.toString()) }) {
                        Text("₹$amt")
                    }
                }
                OutlinedButton(onClick = { onPaidAmountChange(fa.toString()) }) {
                    Text("Full ₹$fa")
                }
            }
        }

        if (message.isNotEmpty()) {
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        errorMessage?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = onSaveBill,
                modifier = Modifier.fillMaxWidth(),
                enabled = billItems.isNotEmpty() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("Save bill")
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}