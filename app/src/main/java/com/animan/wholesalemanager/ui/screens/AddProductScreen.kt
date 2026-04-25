package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.utils.PriceUtils.isValidPriceInput
import com.animan.wholesalemanager.utils.PriceUtils.round2dp
import com.animan.wholesalemanager.viewmodel.ProductViewModel

private val UNIT_OPTIONS = listOf("Piece","Kg","Gram","Liter","ML","Box","Packet","Dozen","Bag","Bottle")
private val GST_OPTIONS  = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null
) {
    val viewModel: ProductViewModel = viewModel()
    val S = AppLanguage.strings

    var name          by remember { mutableStateOf("") }
    var sellingPrice  by remember { mutableStateOf("") }
    var costPrice     by remember { mutableStateOf("") }
    var quantity      by remember { mutableStateOf("") }
    var unit          by remember { mutableStateOf("Piece") }
    var category      by remember { mutableStateOf("") }
    var minStockLevel by remember { mutableStateOf("5") }
    var barcode       by remember { mutableStateOf("") }
    var gstPercent    by remember { mutableStateOf(0.0) }
    var allowPartial  by remember { mutableStateOf(false) }   // ← NEW
    var loaded        by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val isEditMode = productId != null

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
        viewModel.fetchCategories()
    }

    LaunchedEffect(viewModel.productList.value) {
        if (!loaded && isEditMode) {
            viewModel.productList.value.find { it.id == productId }?.let { p ->
                editingProduct = p
                name          = p.name
                sellingPrice  = p.sellingPrice.formatPrice()
                costPrice     = p.costPrice.formatPrice()
                quantity      = formatQty(p.quantity)          // ← Double format
                unit          = p.unit
                category      = p.category
                minStockLevel = formatQty(p.minStockLevel)     // ← Double format
                barcode       = p.barcode
                gstPercent    = p.gstPercent
                allowPartial  = p.allowPartial                 // ← NEW
                loaded        = true
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
                // ── Quantity field — decimal if allowPartial, integer otherwise ──
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { v ->
                        if (allowPartial) {
                            if (isValidPriceInput(v)) quantity = v
                        } else {
                            if (v.all { it.isDigit() }) quantity = v
                        }
                    },
                    label = { Text("${S.quantity} *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowPartial) KeyboardType.Decimal
                        else KeyboardType.Number
                    )
                )
                OutlinedTextField(
                    value = minStockLevel,
                    onValueChange = { v ->
                        if (allowPartial) {
                            if (isValidPriceInput(v)) minStockLevel = v
                        } else {
                            if (v.all { it.isDigit() }) minStockLevel = v
                        }
                    },
                    label = { Text(S.minStock) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowPartial) KeyboardType.Decimal
                        else KeyboardType.Number
                    )
                )
            }

            // ── Sell in fractions toggle ──────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                        checked = allowPartial,
                        onCheckedChange = {
                            allowPartial = it
                            // Reset qty fields to avoid leftover decimals
                            // when switching from partial → whole
                            if (!it) {
                                quantity = quantity.toDoubleOrNull()
                                    ?.toInt()?.toString() ?: quantity
                                minStockLevel = minStockLevel.toDoubleOrNull()
                                    ?.toInt()?.toString() ?: minStockLevel
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
                    val sp  = sellingPrice.toDoubleOrNull()?.round2dp()
                    val cp  = costPrice.toDoubleOrNull()?.round2dp() ?: 0.0
                    val qty = quantity.toDoubleOrNull() ?: 0.0           // ← Double
                    val minStock = minStockLevel.toDoubleOrNull() ?: 5.0 // ← Double

                    if (name.isBlank()) { viewModel.errorMessage.value = S.nameEmpty; return@Button }
                    if (sp == null || sp < 0) { viewModel.errorMessage.value = S.invalidPrice; return@Button }

                    val product = if (isEditMode) {
                        (editingProduct ?: return@Button).copy(
                            name = name.trim(), sellingPrice = sp, costPrice = cp,
                            quantity = qty, unit = unit, category = category.trim(),
                            minStockLevel = minStock, barcode = barcode.trim(),
                            gstPercent = gstPercent, allowPartial = allowPartial  // ← NEW
                        )
                    } else {
                        Product(
                            name = name.trim(), sellingPrice = sp, costPrice = cp,
                            quantity = qty, unit = unit, category = category.trim(),
                            minStockLevel = minStock, barcode = barcode.trim(),
                            gstPercent = gstPercent, allowPartial = allowPartial  // ← NEW
                        )
                    }

                    if (isEditMode) viewModel.updateProduct(product) { navController.popBackStack() }
                    else viewModel.addProduct(product) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (isEditMode) S.update else S.save)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Qty display helper ────────────────────────────────────────────────
// Shows "2" instead of "2.0", "0.5" instead of "0.500000"
fun formatQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString()
    else v.toBigDecimal().stripTrailingZeros().toPlainString()