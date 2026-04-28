package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.data.local.ProductSupplierLink
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.isValidPriceInput
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import com.animan.wholesalemanager.viewmodel.SupplierViewModel

private val UNIT_OPTIONS = listOf("Piece","Kg","Gram","Liter","ML","Box","Packet","Dozen","Bag","Bottle")
private val GST_OPTIONS  = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId    : String? = null
) {
    val viewModel        : ProductViewModel  = viewModel()
    val supplierViewModel: SupplierViewModel = viewModel()
    val S = AppLanguage.strings

    var name           by remember { mutableStateOf("") }
    var sellingPrice   by remember { mutableStateOf("") }
    var costPrice      by remember { mutableStateOf("") }
    var quantity       by remember { mutableStateOf("") }
    var unit           by remember { mutableStateOf("Piece") }
    var category       by remember { mutableStateOf("") }
    var minStockLevel  by remember { mutableStateOf("5") }
    var barcode        by remember { mutableStateOf("") }
    var gstPercent     by remember { mutableStateOf(0.0) }
    var allowPartial   by remember { mutableStateOf(false) }
    var loaded         by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // ── Supplier link dialog state ────────────────────────────────────
    var showLinkDialog    by remember { mutableStateOf(false) }
    var linkToDelete      by remember { mutableStateOf<ProductSupplierLink?>(null) }

    val isEditMode = productId != null

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
        viewModel.fetchCategories()
        supplierViewModel.fetchSuppliers()
        if (isEditMode && productId != null) {
            viewModel.fetchSupplierLinks(productId)
        }
    }

    LaunchedEffect(viewModel.productList.value) {
        if (!loaded && isEditMode) {
            viewModel.productList.value.find { it.id == productId }?.let { p ->
                editingProduct = p
                name           = p.name
                sellingPrice   = p.sellingPrice.formatPrice()
                costPrice      = p.costPrice.formatPrice()
                quantity       = formatQty(p.quantity)
                unit           = p.unit
                category       = p.category
                minStockLevel  = formatQty(p.minStockLevel)
                barcode        = p.barcode
                gstPercent     = p.gstPercent
                allowPartial   = p.allowPartial
                loaded         = true
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isEditMode) S.editProduct else S.addProduct) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(S.productName + " *") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { if (isValidPriceInput(it)) sellingPrice = it },
                    label = { Text("${S.sellingPrice} (₹) *") },
                    modifier = Modifier.weight(1f).onFocusChanged { focus ->
                        if (!focus.isFocused && sellingPrice.isNotBlank()) {
                            sellingPrice = sellingPrice.toDoubleOrNull()
                                ?.round2dp()?.formatPrice() ?: sellingPrice
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { if (isValidPriceInput(it)) costPrice = it },
                    label = { Text("${S.costPrice} (₹)") },
                    modifier = Modifier.weight(1f).onFocusChanged { focus ->
                        if (!focus.isFocused && costPrice.isNotBlank()) {
                            costPrice = costPrice.toDoubleOrNull()
                                ?.round2dp()?.formatPrice() ?: costPrice
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { v ->
                        if (allowPartial) { if (isValidPriceInput(v)) quantity = v }
                        else { if (v.all { it.isDigit() }) quantity = v }
                    },
                    label = { Text("${S.quantity} *") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowPartial) KeyboardType.Decimal
                        else KeyboardType.Number)
                )
                OutlinedTextField(
                    value = minStockLevel,
                    onValueChange = { v ->
                        if (allowPartial) { if (isValidPriceInput(v)) minStockLevel = v }
                        else { if (v.all { it.isDigit() }) minStockLevel = v }
                    },
                    label = { Text(S.minStock) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowPartial) KeyboardType.Decimal
                        else KeyboardType.Number)
                )
            }

            // Sell in fractions toggle
            Surface(
                shape    = MaterialTheme.shapes.medium,
                color    = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sell in fractions",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (allowPartial) "e.g. 0.5 Kg, 0.250 Kg"
                            else "Whole units only (1, 2, 3…)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked         = allowPartial,
                        onCheckedChange = {
                            allowPartial = it
                            if (!it) {
                                quantity      = quantity.toDoubleOrNull()?.toInt()?.toString() ?: quantity
                                minStockLevel = minStockLevel.toDoubleOrNull()?.toInt()?.toString() ?: minStockLevel
                            }
                        }
                    )
                }
            }

            Text("${S.unit} *", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UNIT_OPTIONS.forEach { opt ->
                    FilterChip(selected = unit == opt, onClick = { unit = opt },
                        label = { Text(opt) })
                }
            }

            Text("GST %", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GST_OPTIONS.forEach { opt ->
                    FilterChip(selected = gstPercent == opt, onClick = { gstPercent = opt },
                        label = { Text(if (opt == 0.0) S.noGst else "${opt.toInt()}%") })
                }
            }

            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text(S.category) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (viewModel.categoryList.value.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.categoryList.value.forEach { cat ->
                        SuggestionChip(onClick = { category = cat }, label = { Text(cat) })
                    }
                }
            }

            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text(S.barcode) }, modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val sp       = sellingPrice.toDoubleOrNull()?.round2dp()
                    val cp       = costPrice.toDoubleOrNull()?.round2dp() ?: 0.0
                    val qty      = quantity.toDoubleOrNull() ?: 0.0
                    val minStock = minStockLevel.toDoubleOrNull() ?: 5.0

                    if (name.isBlank())           { viewModel.errorMessage.value = S.nameEmpty;     return@Button }
                    if (sp == null || sp < 0)     { viewModel.errorMessage.value = S.invalidPrice;  return@Button }

                    val product = if (isEditMode) {
                        (editingProduct ?: return@Button).copy(
                            name = name.trim(), sellingPrice = sp, costPrice = cp,
                            quantity = qty, unit = unit, category = category.trim(),
                            minStockLevel = minStock, barcode = barcode.trim(),
                            gstPercent = gstPercent, allowPartial = allowPartial
                        )
                    } else {
                        Product(
                            name = name.trim(), sellingPrice = sp, costPrice = cp,
                            quantity = qty, unit = unit, category = category.trim(),
                            minStockLevel = minStock, barcode = barcode.trim(),
                            gstPercent = gstPercent, allowPartial = allowPartial
                        )
                    }

                    if (isEditMode) viewModel.updateProduct(product) { navController.popBackStack() }
                    else viewModel.addProduct(product) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (isEditMode) S.update else S.save)
            }

            // ── Supplier links section — only in edit mode ────────────
            // (product must be saved before linking suppliers)
            if (isEditMode && productId != null) {
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LocalShipping, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Text("Linked suppliers",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick  = { showLinkDialog = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Link supplier", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (viewModel.supplierLinks.value.isEmpty()) {
                    Text(
                        "No suppliers linked yet. Link a supplier to quickly start a purchase for this product.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    viewModel.supplierLinks.value.forEach { link ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(link.supplierName,
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium)
                                    if (link.costPrice > 0)
                                        Text("Last cost: ₹${link.costPrice.formatPrice()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (link.note.isNotBlank())
                                        Text(link.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // ── Purchase button — navigates to PurchaseScreen
                                    // with this product pre-selected via route arg ──
                                    FilledTonalButton(
                                        onClick  = {
                                            navController.navigate(
                                                "purchase/${link.supplierId}?preselect=${productId}"
                                            )
                                        },
                                        modifier       = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Filled.ShoppingCart, null,
                                            modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Buy", style = MaterialTheme.typography.labelMedium)
                                    }
                                    IconButton(
                                        onClick  = { linkToDelete = link },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, null,
                                            tint     = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!isEditMode) {
                // Hint when adding new product
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null,
                            modifier = Modifier.size(14.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Save the product first, then edit it to link suppliers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Link supplier dialog ──────────────────────────────────────────
    if (showLinkDialog && productId != null) {
        LinkSupplierDialog(
            suppliers      = supplierViewModel.supplierList.value,
            existingLinks  = viewModel.supplierLinks.value,
            onLink         = { supplierId, supplierName, costPrice, note ->
                viewModel.addSupplierLink(
                    ProductSupplierLink(
                        productId    = productId,
                        supplierId   = supplierId,
                        supplierName = supplierName,
                        costPrice    = costPrice,
                        note         = note
                    )
                ) { showLinkDialog = false }
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    // ── Delete link confirm ───────────────────────────────────────────
    linkToDelete?.let { link ->
        AlertDialog(
            onDismissRequest = { linkToDelete = null },
            title   = { Text("Remove supplier?") },
            text    = { Text("Remove ${link.supplierName} from this product's suppliers?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeSupplierLink(link.id, link.productId)
                        linkToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                OutlinedButton(onClick = { linkToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Link supplier dialog ──────────────────────────────────────────────
@Composable
private fun LinkSupplierDialog(
    suppliers     : List<com.animan.wholesalemanager.data.local.Supplier>,
    existingLinks : List<ProductSupplierLink>,
    onLink        : (supplierId: String, supplierName: String,
                     costPrice: Double, note: String) -> Unit,
    onDismiss     : () -> Unit
) {
    val existingSupplierIds = existingLinks.map { it.supplierId }.toSet()
    val available = suppliers.filter { it.id !in existingSupplierIds }

    var selectedSupplier by remember {
        mutableStateOf<com.animan.wholesalemanager.data.local.Supplier?>(null)
    }
    var costPriceInput by remember { mutableStateOf("") }
    var note           by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link supplier") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (available.isEmpty()) {
                    Text(
                        "All suppliers are already linked, or no suppliers exist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Select supplier",
                        style = MaterialTheme.typography.labelMedium)

                    // ── FIX: use Row + horizontalScroll instead of LazyRow ──
                    Row(
                        modifier              = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        available.forEach { supplier ->
                            FilterChip(
                                selected = selectedSupplier?.id == supplier.id,
                                onClick  = { selectedSupplier = supplier },
                                label    = { Text(supplier.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value         = costPriceInput,
                        onValueChange = { v ->
                            if (v.isEmpty() || v.matches(Regex("^\\d*\\.?\\d*$")))
                                costPriceInput = v
                        },
                        label           = { Text("Cost price from this supplier (₹)") },
                        modifier        = Modifier.fillMaxWidth(),
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal),
                        prefix = { Text("₹") }
                    )

                    OutlinedTextField(
                        value         = note,
                        onValueChange = { note = it },
                        label         = { Text("Note (optional)") },
                        modifier      = Modifier.fillMaxWidth(),
                        maxLines      = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sup = selectedSupplier ?: return@Button
                    onLink(
                        sup.id,
                        sup.name,
                        costPriceInput.toDoubleOrNull() ?: 0.0,
                        note.trim()
                    )
                },
                enabled = selectedSupplier != null
            ) { Text("Link") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Qty display helper ────────────────────────────────────────────────
fun formatQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString()
    else v.toBigDecimal().stripTrailingZeros().toPlainString()