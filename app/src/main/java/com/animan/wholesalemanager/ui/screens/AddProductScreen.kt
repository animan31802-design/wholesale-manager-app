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

private val UNIT_OPTIONS = listOf("Piece","Kg","Gram","Liter","ML","Box","Packet","Dozen","Bag","Bottle")
private val GST_OPTIONS  = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavController, productId: String? = null) {
    val viewModel: ProductViewModel = viewModel()

    var name          by remember { mutableStateOf("") }
    var sellingPrice  by remember { mutableStateOf("") }
    var costPrice     by remember { mutableStateOf("") }
    var quantity      by remember { mutableStateOf("") }
    var unit          by remember { mutableStateOf("Piece") }
    var category      by remember { mutableStateOf("") }
    var minStockLevel by remember { mutableStateOf("5") }
    var barcode       by remember { mutableStateOf("") }
    var gstPercent    by remember { mutableStateOf(0.0) }
    var loaded        by remember { mutableStateOf(false) }

    val isEditMode = productId != null

    LaunchedEffect(Unit) { viewModel.fetchProducts(); viewModel.fetchCategories() }

    LaunchedEffect(viewModel.productList.value) {
        if (!loaded && isEditMode) {
            viewModel.productList.value.find { it.id == productId }?.let { p ->
                name = p.name; sellingPrice = p.sellingPrice.toString()
                costPrice = p.costPrice.toString(); quantity = p.quantity.toString()
                unit = p.unit; category = p.category
                minStockLevel = p.minStockLevel.toString()
                barcode = p.barcode; gstPercent = p.gstPercent; loaded = true
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isEditMode) "Edit product" else "Add product") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Product name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it },
                    label = { Text("Selling price (₹) *") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = costPrice, onValueChange = { costPrice = it },
                    label = { Text("Cost price (₹)") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it },
                    label = { Text("Quantity *") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = minStockLevel, onValueChange = { minStockLevel = it },
                    label = { Text("Min stock alert") }, modifier = Modifier.weight(1f),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }

            Text("Unit *", style = MaterialTheme.typography.labelMedium)
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
                        label = { Text(if (opt == 0.0) "No GST" else "${opt.toInt()}%") })
                }
            }

            OutlinedTextField(value = category, onValueChange = { category = it },
                label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (viewModel.categoryList.value.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.categoryList.value.forEach { cat ->
                        SuggestionChip(onClick = { category = cat }, label = { Text(cat) })
                    }
                }
            }

            OutlinedTextField(value = barcode, onValueChange = { barcode = it },
                label = { Text("Barcode (optional)") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val product = Product(id = productId ?: "", name = name.trim(),
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                        quantity = quantity.toIntOrNull() ?: 0, unit = unit,
                        category = category.trim(), minStockLevel = minStockLevel.toIntOrNull() ?: 5,
                        barcode = barcode.trim(), gstPercent = gstPercent)
                    if (isEditMode) viewModel.updateProduct(product) { navController.popBackStack() }
                    else viewModel.addProduct(product) { navController.popBackStack() }
                },
                modifier = Modifier.fillMaxWidth(), enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (isEditMode) "Update product" else "Save product")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}