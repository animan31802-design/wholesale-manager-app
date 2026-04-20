package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(navController: NavController) {
    val viewModel: ProductViewModel = viewModel()

    var searchQuery       by remember { mutableStateOf("") }
    var deleteTargetId    by remember { mutableStateOf<String?>(null) }
    var deleteTargetName  by remember { mutableStateOf("") }
    var restockProduct    by remember { mutableStateOf<Product?>(null) }
    var restockQty        by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
        viewModel.fetchCategories()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) viewModel.fetchProducts()
        else viewModel.searchProducts(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Products") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_product") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add product")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search products…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            if (viewModel.isLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                items(viewModel.productList.value, key = { it.id }) { product ->
                    val isLowStock = product.quantity <= product.minStockLevel

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLowStock)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Sell: ₹${product.sellingPrice}  |  Cost: ₹${product.costPrice}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Stock: ${product.quantity} ${product.unit}" +
                                                if (isLowStock) "  ⚠ Low" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLowStock) MaterialTheme.colorScheme.error
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
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { navController.navigate("edit_product/${product.id}") },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Edit") }

                                OutlinedButton(
                                    onClick = { restockProduct = product; restockQty = "" },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Restock") }

                                Button(
                                    onClick = {
                                        deleteTargetId = product.id
                                        deleteTargetName = product.name
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Delete") }
                            }
                        }
                    }
                }
            }

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp))
            }
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────
    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text("Delete product") },
            text = { Text("Delete \"$deleteTargetName\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteProduct(deleteTargetId!!) {}; deleteTargetId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            }
        )
    }

    // ── Restock dialog ────────────────────────────────────────────────
    restockProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { restockProduct = null },
            title = { Text("Restock — ${product.name}") },
            text = {
                Column {
                    Text("Current stock: ${product.quantity} ${product.unit}")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = restockQty,
                        onValueChange = { restockQty = it },
                        label = { Text("Quantity to add") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val qty = restockQty.toIntOrNull() ?: 0
                    if (qty > 0) {
                        viewModel.restockProduct(product.id, qty) {}
                        restockProduct = null
                    }
                }) { Text("Add stock") }
            },
            dismissButton = {
                OutlinedButton(onClick = { restockProduct = null }) { Text("Cancel") }
            }
        )
    }
}