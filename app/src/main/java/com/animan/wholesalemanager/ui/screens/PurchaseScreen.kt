package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.data.local.Purchase
import com.animan.wholesalemanager.data.local.PurchaseItem
import com.animan.wholesalemanager.data.local.Supplier
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import com.animan.wholesalemanager.viewmodel.SupplierViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    supplierId : String,
    onPurchaseSaved: () -> Unit,
    onBack: () -> Unit
) {
    val supplierViewModel: SupplierViewModel = viewModel()
    val productViewModel : ProductViewModel  = viewModel()
    val context          = LocalContext.current
    val scope            = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // ── State ─────────────────────────────────────────────────────────
    var supplier       by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery    by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCart       by remember { mutableStateOf(false) }
    var paidAmount     by remember { mutableStateOf("") }
    var note           by remember { mutableStateOf("") }
    var message        by remember { mutableStateOf("") }
    var isSaving       by remember { mutableStateOf(false) }

    // cart: productId → Pair(qty, costPrice)
    val cartQty   = remember { mutableStateMapOf<String, Double>() }
    val cartPrice = remember { mutableStateMapOf<String, Double>() }  // editable cost price

    var qtyDialogProduct by remember { mutableStateOf<Product?>(null) }

    // ── Load data ─────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        supplierViewModel.fetchSuppliers()
        productViewModel.fetchCategories()
        productViewModel.fetchProducts()
        delay(100)
        supplier = supplierViewModel.supplierList.value.find { it.id == supplierId }
        delay(200); searchFocusRequester.requestFocus()
    }

    // Re-resolve supplier after list loads
    LaunchedEffect(supplierViewModel.supplierList.value) {
        supplier = supplierViewModel.supplierList.value.find { it.id == supplierId }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) productViewModel.fetchProducts()
        else { delay(300); productViewModel.searchProducts(searchQuery) }
    }

    LaunchedEffect(selectedCategory) {
        if (searchQuery.isBlank()) productViewModel.filterByCategory(selectedCategory)
    }

    // ── Derived ───────────────────────────────────────────────────────
    val purchaseItems: List<PurchaseItem> by remember {
        derivedStateOf {
            productViewModel.productList.value
                .filter { (cartQty[it.id] ?: 0.0) > 0.0 }
                .map { p ->
                    val qty   = cartQty[p.id] ?: 1.0
                    val price = cartPrice[p.id] ?: p.costPrice
                    PurchaseItem(
                        productId         = p.id,
                        name              = p.name,
                        costPrice         = price,
                        previousCostPrice = p.costPrice,
                        quantity          = qty,
                        unit              = p.unit,
                        gstPercent        = p.gstPercent
                    )
                }
        }
    }

    val itemsTotal by remember { derivedStateOf { purchaseItems.sumOf { it.costPrice * it.quantity } } }
    val gstTotal   by remember { derivedStateOf { purchaseItems.sumOf { it.gstAmount } } }
    val grandTotal by remember { derivedStateOf { itemsTotal + gstTotal } }

    val filteredProducts = productViewModel.productList.value

    // ── Saving overlay ────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Purchase stock")
                            supplier?.let {
                                Text(it.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        // Cart toggle button with badge
                        BadgedBox(badge = {
                            val count = cartQty.values.count { it > 0 }
                            if (count > 0) Badge { Text(count.toString()) }
                        }) {
                            Button(
                                onClick = { showCart = !showCart },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(if (showCart) "Products" else "Cart ${grandTotal.toRupees()}")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (showCart) {
                PurchaseCartView(
                    items              = purchaseItems,
                    supplier           = supplier,
                    itemsTotal         = itemsTotal,
                    gstTotal           = gstTotal,
                    grandTotal         = grandTotal,
                    paidAmount         = paidAmount,
                    onPaidAmountChange = { paidAmount = it; message = "" },
                    note               = note,
                    onNoteChange       = { note = it },
                    message            = message,
                    isSaving           = isSaving,
                    errorMessage       = supplierViewModel.errorMessage.value,
                    onQtyChange        = { item, newQty ->
                        if (newQty <= 0.0) cartQty.remove(item.productId)
                        else cartQty[item.productId] = newQty
                    },
                    onPriceChange      = { item, newPrice ->
                        cartPrice[item.productId] = newPrice
                    },
                    onRemove           = {
                        cartQty.remove(it.productId)
                        cartPrice.remove(it.productId)
                    },
                    onQtyTapped        = { item ->
                        val product = productViewModel.productList.value
                            .find { it.id == item.productId }
                        if (product != null) qtyDialogProduct = product
                    },
                    modifier           = Modifier.padding(padding),
                    onSavePurchase     = {
                        // Validation
                        if (purchaseItems.isEmpty()) {
                            message = "Add at least one product"; return@PurchaseCartView
                        }
                        val sup = supplier
                        if (sup == null) { message = "Supplier not found"; return@PurchaseCartView }
                        val paid = paidAmount.toDoubleOrNull()
                        if (paid == null)      { message = "Enter a valid paid amount"; return@PurchaseCartView }
                        if (paid < 0)          { message = "Cannot be negative"; return@PurchaseCartView }
                        if (paid > grandTotal) { message = "Exceeds total amount"; return@PurchaseCartView }

                        isSaving = true

                        val itemsSnapshot = purchaseItems.toList()
                        val paidSnapshot  = paid
                        val supSnapshot   = sup

                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val purchase = Purchase(
                                        supplierId   = supSnapshot.id,
                                        supplierName = supSnapshot.name,
                                        poNumber     = supplierViewModel.generatePoNumber(),
                                        items        = itemsSnapshot,
                                        itemsTotal   = itemsTotal,
                                        gstTotal     = gstTotal,
                                        grandTotal   = grandTotal,
                                        paidAmount   = paidSnapshot,
                                        balance      = grandTotal - paidSnapshot,
                                        note         = note.trim()
                                    )
                                    supplierViewModel.savePurchaseSuspend(purchase, supSnapshot)
                                }
                                // Re-fetch products so stock is updated in UI
                                productViewModel.fetchProducts()
                                isSaving = false
                                cartQty.clear()
                                cartPrice.clear()
                                onPurchaseSaved()
                            } catch (e: Exception) {
                                message  = "Failed to save: ${e.message}"
                                isSaving = false
                            }
                        }
                    }
                )
            } else {
                // ── Product search + list ─────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it; selectedCategory = "All" },
                        label         = { Text("Search product, category, barcode…") },
                        modifier      = Modifier
                            .fillMaxWidth()
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
                            keyboardController?.hide()
                        })
                    )

                    // Category chips
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf("All") + productViewModel.categoryList.value
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick  = { selectedCategory = cat; searchQuery = "" },
                                label    = { Text(cat) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    when {
                        productViewModel.isLoading.value ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        filteredProducts.isEmpty() ->
                            Box(Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center) {
                                Text("No products found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        else -> LazyColumn {
                            items(filteredProducts, key = { it.id }) { p ->
                                PurchaseProductRow(
                                    product     = p,
                                    qtyInCart   = cartQty[p.id] ?: 0.0,
                                    onAdd       = {
                                        val cur = cartQty[p.id] ?: 0.0
                                        if (cur == 0.0) {
                                            // initialise cost price from product
                                            cartPrice[p.id] = p.costPrice
                                            qtyDialogProduct = p
                                        } else {
                                            cartQty[p.id] = cur + 1.0
                                        }
                                    },
                                    onRemove    = {
                                        val cur = cartQty[p.id] ?: 0.0
                                        if (cur <= 1.0) {
                                            cartQty.remove(p.id)
                                            cartPrice.remove(p.id)
                                        } else cartQty[p.id] = cur - 1.0
                                    },
                                    onQtyTapped = { qtyDialogProduct = p }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // Saving overlay
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
                        Text("Saving purchase…",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Updating stock & expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ── Qty input dialog ──────────────────────────────────────────────
    qtyDialogProduct?.let { product ->
        PurchaseQtyDialog(
            product      = product,
            currentQty   = cartQty[product.id] ?: 0.0,
            currentPrice = cartPrice[product.id] ?: product.costPrice,
            onConfirm    = { qty, price ->
                if (qty <= 0.0) {
                    cartQty.remove(product.id)
                    cartPrice.remove(product.id)
                } else {
                    cartQty[product.id]   = qty
                    cartPrice[product.id] = price
                }
                qtyDialogProduct = null
            },
            onDismiss = { qtyDialogProduct = null }
        )
    }
}

// ── Product row ───────────────────────────────────────────────────────
@Composable
private fun PurchaseProductRow(
    product    : Product,
    qtyInCart  : Double,
    onAdd      : () -> Unit,
    onRemove   : () -> Unit,
    onQtyTapped: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall)
            val gstText = if (product.gstPercent > 0) "  GST ${product.gstPercent.toInt()}%" else ""
            Text(
                "Cost: ₹${product.costPrice.formatPrice()}  |  ${product.unit}" +
                        "  |  Stock: ${formatQty(product.quantity)}$gstText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (product.category.isNotBlank())
                Text(product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
        }

        if (qtyInCart > 0.0) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledIconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Text("-")
                }
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onQtyTapped() }
                ) {
                    Text(
                        text       = formatQty(qtyInCart),
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                FilledIconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Text("+")
                }
            }
        } else {
            Button(onClick = onAdd, modifier = Modifier.height(36.dp)) {
                Text("Add")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

// ── Qty + price dialog ────────────────────────────────────────────────
@Composable
private fun PurchaseQtyDialog(
    product      : Product,
    currentQty   : Double,
    currentPrice : Double,
    onConfirm    : (qty: Double, price: Double) -> Unit,
    onDismiss    : () -> Unit
) {
    var qtyInput   by remember { mutableStateOf(if (currentQty > 0) formatQty(currentQty) else "") }
    var priceInput by remember { mutableStateOf(if (currentPrice > 0) currentPrice.formatPrice() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(product.name,
                style = MaterialTheme.typography.titleMedium, maxLines = 1)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter quantity and cost price for this purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value         = qtyInput,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) qtyInput = v
                    },
                    label          = { Text("Quantity") },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier       = Modifier.fillMaxWidth(),
                    suffix         = { Text(product.unit) }
                )

                OutlinedTextField(
                    value         = priceInput,
                    onValueChange = { v ->
                        if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) priceInput = v
                    },
                    label          = { Text("Cost price per ${product.unit}") },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier       = Modifier.fillMaxWidth(),
                    prefix         = { Text("₹") }
                )

                // Show if price changed
                val newPrice = priceInput.toDoubleOrNull() ?: 0.0
                if (product.costPrice > 0 && newPrice > 0 && newPrice != product.costPrice) {
                    val diff    = newPrice - product.costPrice
                    val isUp    = diff > 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isUp) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isUp) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isUp) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Price ${if (isUp) "increased" else "decreased"} from " +
                                        "₹${product.costPrice.formatPrice()} " +
                                        "(${if (isUp) "+" else ""}${diff.formatPrice()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUp) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty   = qtyInput.toDoubleOrNull() ?: 0.0
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    onConfirm(qty, price)
                },
                enabled = (qtyInput.toDoubleOrNull() ?: 0.0) > 0.0 &&
                        (priceInput.toDoubleOrNull() ?: 0.0) > 0.0
            ) { Text("Confirm") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Cart view ─────────────────────────────────────────────────────────
@Composable
private fun PurchaseCartView(
    items              : List<PurchaseItem>,
    supplier           : Supplier?,
    itemsTotal         : Double,
    gstTotal           : Double,
    grandTotal         : Double,
    paidAmount         : String,
    onPaidAmountChange : (String) -> Unit,
    note               : String,
    onNoteChange       : (String) -> Unit,
    message            : String,
    isSaving           : Boolean,
    errorMessage       : String?,
    onQtyChange        : (PurchaseItem, Double) -> Unit,
    onPriceChange      : (PurchaseItem, Double) -> Unit,
    onRemove           : (PurchaseItem) -> Unit,
    onQtyTapped        : (PurchaseItem) -> Unit,
    modifier           : Modifier = Modifier,
    onSavePurchase     : () -> Unit
) {
    LazyColumn(
        modifier        = modifier.fillMaxSize(),
        contentPadding  = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (items.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("No items added. Go back and select products.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@LazyColumn
        }

        // PO info card
        supplier?.let { sup ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Supplier", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(sup.name, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            if (sup.phone.isNotBlank())
                                Text(sup.phone, style = MaterialTheme.typography.bodySmall)
                        }
                        if (sup.balance > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Existing due",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                                Text(sup.balance.toRupees(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Purchase items
        items(items) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name,
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("${formatQty(item.quantity)} ${item.unit} × ₹${item.costPrice.formatPrice()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.gstPercent > 0)
                                Text("GST ${item.gstPercent}% = ₹${item.gstAmount.formatPrice()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Line total: ₹${item.lineTotal.formatPrice()}",
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color      = MaterialTheme.colorScheme.primary)
                            // Price changed indicator
                            if (item.costPrice != item.previousCostPrice) {
                                val diff = item.costPrice - item.previousCostPrice
                                Text(
                                    "Cost price updated from ₹${item.previousCostPrice.formatPrice()}" +
                                            " (${if (diff > 0) "+" else ""}${diff.formatPrice()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (diff > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Filled.Close, null,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Inline qty + price editors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Qty stepper
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier              = Modifier.weight(1f)
                        ) {
                            FilledIconButton(
                                onClick  = {
                                    val cur = item.quantity
                                    if (cur <= 1.0) onRemove(item)
                                    else onQtyChange(item, cur - 1.0)
                                },
                                modifier = Modifier.size(30.dp)
                            ) { Text("-") }
                            Surface(
                                shape    = MaterialTheme.shapes.small,
                                color    = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.clickable { onQtyTapped(item) }
                            ) {
                                Text(
                                    formatQty(item.quantity),
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            FilledIconButton(
                                onClick  = { onQtyChange(item, item.quantity + 1.0) },
                                modifier = Modifier.size(30.dp)
                            ) { Text("+") }
                        }

                        // Editable cost price
                        var priceEdit by remember(item.productId) {
                            mutableStateOf(item.costPrice.formatPrice())
                        }
                        OutlinedTextField(
                            value         = priceEdit,
                            onValueChange = { v ->
                                if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    priceEdit = v
                                    val p = v.toDoubleOrNull()
                                    if (p != null && p > 0) onPriceChange(item, p)
                                }
                            },
                            label   = { Text("₹/unit") },
                            modifier = Modifier.width(110.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix  = { Text("₹") }
                        )
                    }
                }
            }
        }

        // Summary card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Items total"); Text(itemsTotal.toRupees())
                    }
                    if (gstTotal > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("GST", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gstTotal.toRupees(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Grand total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(grandTotal.toRupees(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Paid amount
        item {
            OutlinedTextField(
                value         = paidAmount,
                onValueChange = onPaidAmountChange,
                label         = { Text("Amount paid to supplier (₹)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        // Quick amounts
        item {
            val fa = grandTotal.round2dp()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500, 1000, 2000, 5000).filter { it <= fa }.forEach { amt ->
                    OutlinedButton(onClick = { onPaidAmountChange(amt.toString()) }) {
                        Text("₹$amt")
                    }
                }
                OutlinedButton(onClick = { onPaidAmountChange(fa.toString()) }) {
                    Text("Full")
                }
            }
        }

        // Balance info
        item {
            val paid = paidAmount.toDoubleOrNull() ?: 0.0
            val due  = (grandTotal - paid).round2dp()
            if (due > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Balance due to supplier",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(due.toRupees(),
                            color      = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Note
        item {
            OutlinedTextField(
                value         = note,
                onValueChange = onNoteChange,
                label         = { Text("Note (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                maxLines      = 2
            )
        }

        if (message.isNotEmpty()) item {
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        errorMessage?.let { item {
            Text(it, color = MaterialTheme.colorScheme.error)
        }}

        // Save button
        item {
            Button(
                onClick  = { onSavePurchase() },
                modifier = Modifier.fillMaxWidth(),
                enabled  = items.isNotEmpty() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save purchase")
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}