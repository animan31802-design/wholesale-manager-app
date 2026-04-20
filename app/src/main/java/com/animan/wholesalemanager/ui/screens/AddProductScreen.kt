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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.viewmodel.ProductViewModel

// Common unit options for a wholesale store
private val UNIT_OPTIONS = listOf("Piece", "Kg", "Gram", "Liter", "ML", "Box", "Packet", "Dozen", "Bag", "Bottle")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null
) {
    val viewModel: ProductViewModel = viewModel()

    var name          by remember { mutableStateOf("") }
    var sellingPrice  by remember { mutableStateOf("") }
    var costPrice     by remember { mutableStateOf("") }
    var quantity      by remember { mutableStateOf("") }
    var unit          by remember { mutableStateOf("Piece") }
    var category      by remember { mutableStateOf("") }
    var minStockLevel by remember { mutableStateOf("5") }
    var barcode       by remember { mutableStateOf("") }
    var loaded        by remember { mutableStateOf(false) }

    val isEditMode = productId != null

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
        viewModel.fetchCategories()
    }

    // Pre-fill in edit mode
    LaunchedEffect(viewModel.productList.value) {
        if (!loaded && isEditMode) {
            viewModel.productList.value.find { it.id == productId }?.let { p ->
                name          = p.name
                sellingPrice  = p.sellingPrice.toString()
                costPrice     = p.costPrice.toString()
                quantity      = p.quantity.toString()
                unit          = p.unit
                category      = p.category
                minStockLevel = p.minStockLevel.toString()
                barcode       = p.barcode
                loaded        = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit product" else "Add product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Product Name ──────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Selling Price + Cost Price (side by side) ─────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Selling price (₹) *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Cost price (₹)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // ── Quantity + Min Stock Level ────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = minStockLevel,
                    onValueChange = { minStockLevel = it },
                    label = { Text("Min stock alert") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // ── Unit selector ─────────────────────────────────────────
            Text("Unit *", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UNIT_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = unit == option,
                        onClick = { unit = option },
                        label = { Text(option) }
                    )
                }
            }

            // ── Category ──────────────────────────────────────────────
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g. Rice, Oil, Beverages)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Show existing categories as quick-select chips
            if (viewModel.categoryList.value.isNotEmpty()) {
                Text("Existing categories:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    viewModel.categoryList.value.forEach { cat ->
                        SuggestionChip(
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            // ── Barcode (optional) ────────────────────────────────────
            OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // ── Error ─────────────────────────────────────────────────
            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // ── Save button ───────────────────────────────────────────
            Button(
                onClick = {
                    val product = Product(
                        id            = productId ?: "",
                        name          = name.trim(),
                        sellingPrice  = sellingPrice.toDoubleOrNull() ?: 0.0,
                        costPrice     = costPrice.toDoubleOrNull() ?: 0.0,
                        quantity      = quantity.toIntOrNull() ?: 0,
                        unit          = unit,
                        category      = category.trim(),
                        minStockLevel = minStockLevel.toIntOrNull() ?: 5,
                        barcode       = barcode.trim()
                    )
                    if (isEditMode) {
                        viewModel.updateProduct(product) { navController.popBackStack() }
                    } else {
                        viewModel.addProduct(product) { navController.popBackStack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (isEditMode) "Update product" else "Save product")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}