package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.printer.PrinterManager
import com.animan.wholesalemanager.ui.components.UpiPaymentDialog
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SortMode { NAME, FREQUENT }

@Composable
fun BillingScreen(customer: Customer, onBillCreated: () -> Unit) {

    val customerViewModel: CustomerViewModel = viewModel()
    val productViewModel: ProductViewModel   = viewModel()
    val printerManager   = remember { PrinterManager() }
    val context          = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val scope            = rememberCoroutineScope()

    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortMode         by remember { mutableStateOf(SortMode.FREQUENT) }
    var paidAmount       by remember { mutableStateOf("") }
    var message          by remember { mutableStateOf("") }
    var showCart         by remember { mutableStateOf(false) }
    var isSaving         by remember { mutableStateOf(false) }   // ← single source of truth
    val cartQty          = remember { mutableStateMapOf<String, Double>() }

    var qtyDialogProduct by remember { mutableStateOf<Product?>(null) }

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

    val sortedProducts by remember {
        derivedStateOf {
            when (sortMode) {
                SortMode.NAME     -> productViewModel.productList.value.sortedBy { it.name }
                SortMode.FREQUENT -> {
                    val frequent    = productViewModel.frequentList.value
                    val frequentIds = frequent.map { it.id }.toSet()
                    frequent + productViewModel.productList.value
                        .filter { it.id !in frequentIds }.sortedBy { it.name }
                }
            }
        }
    }

    val billItems: List<BillItem> by remember {
        derivedStateOf {
            productViewModel.productList.value
                .filter { (cartQty[it.id] ?: 0.0) > 0.0 }
                .map { p -> BillItem(
                    productId  = p.id, name = p.name, price = p.sellingPrice,
                    costPrice  = p.costPrice, unit = p.unit,
                    quantity   = cartQty[p.id] ?: 1.0,
                    gstPercent = p.gstPercent
                )}
        }
    }

    val itemsTotal by remember { derivedStateOf { billItems.sumOf { it.price * it.quantity } } }
    val gstTotal   by remember { derivedStateOf { billItems.sumOf { it.gstAmount } } }
    val grandTotal by remember { derivedStateOf { itemsTotal + gstTotal } }
    val totalOwed  = grandTotal + customer.balance

    fun addToCart(p: Product) {
        val cur = cartQty[p.id] ?: 0.0
        if (cur < p.quantity) {
            if (p.allowPartial) {
                if (cur == 0.0) qtyDialogProduct = p else cartQty[p.id] = cur + 1.0
            } else {
                cartQty[p.id] = cur + 1.0
            }
        }
    }

    fun removeFromCart(p: Product) {
        val cur = cartQty[p.id] ?: 0.0
        when {
            cur <= 1.0 -> cartQty.remove(p.id)
            else       -> cartQty[p.id] = cur - 1.0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // ── Top bar ───────────────────────────────────────────────────
            Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically) {
                        Column {
                            Text(customer.name, style = MaterialTheme.typography.titleMedium)
                            if (customer.balance > 0)
                                Text("Prev. balance: ${customer.balance.toRupees()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                        }
                        BadgedBox(badge = {
                            val totalItems = cartQty.values.sum()
                            if (totalItems > 0) Badge { Text(formatQty(totalItems)) }
                        }) {
                            Button(onClick = { showCart = !showCart }) {
                                Text(if (showCart) "Products" else "Cart ${grandTotal.toRupees()}")
                            }
                        }
                    }
                }
            }

            if (showCart) {
                CartView(
                    billItems          = billItems,
                    customer           = customer,
                    itemsTotal         = itemsTotal,
                    gstTotal           = gstTotal,
                    grandTotal         = grandTotal,
                    totalOwed          = totalOwed,
                    paidAmount         = paidAmount,
                    onPaidAmountChange = { paidAmount = it; message = "" },
                    message            = message,
                    isLoading          = customerViewModel.isLoading.value,
                    isSaving           = isSaving,
                    errorMessage       = customerViewModel.errorMessage.value,
                    onQtyDecrease      = { item ->
                        val cur = cartQty[item.productId] ?: 1.0
                        if (cur <= 1.0) cartQty.remove(item.productId)
                        else cartQty[item.productId] = cur - 1.0
                    },
                    onQtyIncrease      = { item ->
                        val avail = productViewModel.productList.value
                            .find { it.id == item.productId }?.quantity ?: 0.0
                        val cur = cartQty[item.productId] ?: 1.0
                        if (cur < avail) cartQty[item.productId] = cur + 1.0
                    },
                    onQtyTapped        = { item ->
                        val product = productViewModel.productList.value
                            .find { it.id == item.productId }
                        if (product != null) qtyDialogProduct = product
                    },
                    onRemove           = { cartQty.remove(it.productId) },
                    onSaveBill         = {
                        // Validation
                        if (billItems.isEmpty()) { message = "Add at least one product"; return@CartView }
                        val paid = paidAmount.toDoubleOrNull()
                        if (paid == null)     { message = "Enter a valid paid amount"; return@CartView }
                        if (paid < 0)         { message = "Cannot be negative"; return@CartView }
                        if (paid > totalOwed) { message = "Exceeds total amount"; return@CartView }

                        isSaving = true   // triggers overlay + disables button immediately

                        val itemsSnapshot = billItems.toList()
                        val paidSnapshot  = paid

                        scope.launch {
                            // Step 1: save to SQLite off main thread
                            try {
                                withContext(Dispatchers.IO) {
                                    customerViewModel.createBillSuspend(
                                        customer, itemsSnapshot,
                                        itemsTotal, gstTotal, grandTotal, paidSnapshot
                                    )
                                }
                            } catch (e: Exception) {
                                message  = "Failed to save bill: ${e.message}"
                                isSaving = false
                                return@launch
                            }

                            // Step 2: print off main thread (Bluetooth blocks)
                            val printResult = withContext(Dispatchers.IO) {
                                printerManager.printBill(
                                    context, customer, itemsSnapshot,
                                    itemsTotal, paidSnapshot, totalOwed
                                )
                            }

                            // Step 3: update UI on main thread
                            message  = if (printResult == "Printed successfully")
                                "Bill saved & printed" else "Bill saved"
                            isSaving = false
                            cartQty.clear()
                            onBillCreated()
                        }
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it; selectedCategory = "All" },
                        label         = { Text("Search product, category, barcode…") },
                        modifier      = Modifier.fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .focusRequester(searchFocusRequester),
                        singleLine    = true,
                        leadingIcon   = { Icon(Icons.Filled.Search, null) },
                        trailingIcon  = {
                            if (searchQuery.isNotEmpty())
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, null)
                                }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            sortedProducts.firstOrNull()?.let { addToCart(it) }
                            keyboardController?.hide()
                        })
                    )

                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(selected = sortMode == SortMode.FREQUENT,
                                onClick = { sortMode = SortMode.FREQUENT },
                                label   = { Text("Most sold") })
                        }
                        item {
                            FilterChip(selected = sortMode == SortMode.NAME,
                                onClick = { sortMode = SortMode.NAME },
                                label   = { Text("A→Z") })
                        }
                        item { Spacer(Modifier.width(4.dp)) }
                        val categories = listOf("All") + productViewModel.categoryList.value
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick  = { selectedCategory = cat; searchQuery = "" },
                                label    = { Text(cat) })
                        }
                    }

                    if (sortMode == SortMode.FREQUENT &&
                        searchQuery.isBlank() &&
                        selectedCategory == "All" &&
                        productViewModel.frequentList.value.isNotEmpty()) {
                        Text("  Frequently sold",
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp))
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(productViewModel.frequentList.value) { p ->
                                FrequentProductChip(p, cartQty[p.id] ?: 0.0) { addToCart(p) }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    when {
                        productViewModel.isLoading.value ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            }
                        sortedProducts.isEmpty() && searchQuery.isNotBlank() ->
                            Box(Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center) {
                                Text("No products found for \"$searchQuery\"",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        else -> LazyColumn {
                            items(sortedProducts, key = { it.id }) { p ->
                                ProductBillingRow(
                                    product     = p,
                                    qtyInCart   = cartQty[p.id] ?: 0.0,
                                    onAdd       = { addToCart(p) },
                                    onRemove    = { removeFromCart(p) },
                                    onQtyTapped = { qtyDialogProduct = p }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Saving overlay — drawn on top of CartView ────────────────────
        if (isSaving) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape     = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Saving & printing…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

    } // close Box

    qtyDialogProduct?.let { product ->
        QtyInputDialog(
            product    = product,
            currentQty = cartQty[product.id] ?: 0.0,
            onConfirm  = { newQty ->
                if (newQty <= 0.0) cartQty.remove(product.id)
                else cartQty[product.id] = newQty
                qtyDialogProduct = null
            },
            onDismiss  = { qtyDialogProduct = null }
        )
    }
}

// ── Qty input dialog ──────────────────────────────────────────────────
@Composable
private fun QtyInputDialog(
    product: Product,
    currentQty: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember {
        mutableStateOf(if (currentQty > 0.0) formatQty(currentQty) else "")
    }
    val isPartial = product.allowPartial

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter quantity (${product.unit})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value         = input,
                    onValueChange = { v ->
                        if (isPartial) {
                            if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) input = v
                        } else {
                            if (v.all { it.isDigit() }) input = v
                        }
                    },
                    label          = { Text("Qty") },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPartial) KeyboardType.Decimal else KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    suffix   = { Text(product.unit) }
                )
                Text("Available: ${formatQty(product.quantity)} ${product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val qty    = input.toDoubleOrNull() ?: 0.0
                    val capped = qty.coerceAtMost(product.quantity)
                    onConfirm(capped)
                },
                enabled = input.isNotBlank() && (input.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Set qty") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Product row in billing list ───────────────────────────────────────
@Composable
private fun ProductBillingRow(
    product: Product,
    qtyInCart: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onQtyTapped: () -> Unit
) {
    val outOfStock = product.quantity <= 0.0
    val atLimit    = qtyInCart >= product.quantity

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {

        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            val gstText = if (product.gstPercent > 0) "  GST ${product.gstPercent.toInt()}%" else ""
            Text(
                "₹${product.sellingPrice.formatPrice()}  |  ${product.unit}" +
                        "  |  Stock: ${formatQty(product.quantity)}$gstText",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.quantity <= product.minStockLevel)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (product.category.isNotBlank())
                Text(product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
        }

        if (qtyInCart > 0.0) {
            Row(verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledIconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Text("-") }
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onQtyTapped() }
                ) {
                    Text(
                        text     = formatQty(qtyInCart),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                FilledIconButton(onClick = onAdd, modifier = Modifier.size(32.dp),
                    enabled = !atLimit) { Text("+") }
            }
        } else {
            Button(onClick = onAdd, enabled = !outOfStock, modifier = Modifier.height(36.dp)) {
                Text(if (outOfStock) "Out of stock" else "Add")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

// ── Frequent product chip ─────────────────────────────────────────────
@Composable
private fun FrequentProductChip(product: Product, qtyInCart: Double, onAdd: () -> Unit) {
    ElevatedCard(
        onClick  = onAdd,
        enabled  = product.quantity > qtyInCart,
        modifier = Modifier.width(110.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(product.name.take(14), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(product.sellingPrice.toRupees(), style = MaterialTheme.typography.bodySmall)
            if (qtyInCart > 0.0) Text("In cart: ${formatQty(qtyInCart)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Cart view ─────────────────────────────────────────────────────────
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
    isSaving: Boolean,      // ← from BillingScreen — NO local override
    errorMessage: String?,
    onQtyDecrease: (BillItem) -> Unit,
    onQtyIncrease: (BillItem) -> Unit,
    onQtyTapped: (BillItem) -> Unit,
    onRemove: (BillItem) -> Unit,
    onSaveBill: () -> Unit
) {
    var showUpiDialog by remember { mutableStateOf(false) }
    var upiAmount     by remember { mutableStateOf(0.0) }

    // !! NO local isSaving declaration here — use the parameter only !!

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                    verticalAlignment     = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "₹${item.price.formatPrice()} × ${formatQty(item.quantity)}" +
                                    " ${item.unit} = ₹${(item.price * item.quantity).formatPrice()}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (item.gstPercent > 0)
                            Text("GST ${item.gstPercent}% = ₹${item.gstAmount.formatPrice()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically) {
                        FilledIconButton(onClick = { onQtyDecrease(item) },
                            Modifier.size(32.dp)) { Text("-") }
                        Surface(
                            shape    = MaterialTheme.shapes.small,
                            color    = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onQtyTapped(item) }
                        ) {
                            Text(
                                text     = formatQty(item.quantity),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        FilledIconButton(onClick = { onQtyIncrease(item) },
                            Modifier.size(32.dp)) { Text("+") }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Items total"); Text(itemsTotal.toRupees())
                    }
                    if (gstTotal > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("GST", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gstTotal.toRupees(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (customer.balance > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Previous balance", color = MaterialTheme.colorScheme.error)
                            Text(customer.balance.toRupees(), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total payable", style = MaterialTheme.typography.titleMedium)
                        Text(totalOwed.toRupees(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value         = paidAmount,
                onValueChange = onPaidAmountChange,
                label         = { Text("Amount paid (₹)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        item {
            val fa = totalOwed.round2dp()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1000, 2000).filter { it <= fa }.forEach { amt ->
                    OutlinedButton(onClick = { onPaidAmountChange(amt.toString()) }) {
                        Text("₹$amt")
                    }
                }
                OutlinedButton(onClick = { onPaidAmountChange(fa.toString()) }) {
                    Text("Full ₹${fa.formatPrice()}")
                }
            }
        }

        item {
            OutlinedButton(
                onClick  = {
                    upiAmount = totalOwed.round2dp() - (paidAmount.toDoubleOrNull() ?: 0.0)
                    if (upiAmount > 0) showUpiDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pay via UPI QR")
            }
        }

        if (message.isNotEmpty()) item {
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        errorMessage?.let { item {
            Text(it, color = MaterialTheme.colorScheme.error)
        }}

        // ── Save bill button ──────────────────────────────────────────
        item {
            Button(
                onClick  = { onSaveBill() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = billItems.isNotEmpty() && !isSaving   // ← uses parameter
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Saving & printing…")
                } else {
                    Text("Save bill")
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showUpiDialog) {
        UpiPaymentDialog(
            amount     = upiAmount,
            billNote   = "Invoice",
            onMarkPaid = {
                onPaidAmountChange(upiAmount.formatPrice())
                showUpiDialog = false
            },
            onDismiss  = { showUpiDialog = false }
        )
    }
}