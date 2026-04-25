package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
import com.animan.wholesalemanager.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(navController: NavController) {
    val viewModel: ProductViewModel = viewModel()

    var searchQuery      by remember { mutableStateOf("") }
    var deleteTargetId   by remember { mutableStateOf<String?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }
    var restockTarget    by remember { mutableStateOf<Product?>(null) }
    var restockQty       by remember { mutableStateOf("") }

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
                        Icon(Icons.Filled.ArrowBack, null)
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Search bar — same pill shape as CustomerListScreen ────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search products…") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine    = true,
                shape         = RoundedCornerShape(50),
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null)
                        }
                }
            )

            if (viewModel.isLoading.value)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // ── Empty state ───────────────────────────────────────────
            if (viewModel.productList.value.isEmpty() && !viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Inventory2, null,
                            modifier = Modifier.size(56.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No products yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { navController.navigate("add_product") }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add first product")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.productList.value, key = { it.id }) { product ->
                        val isLowStock = product.quantity <= product.minStockLevel

                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column {
                                // ── Coloured top strip ────────────────
                                // Red when low stock, primary colour otherwise
                                // Mirrors the customer card's due/cleared strip
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(
                                            if (isLowStock)
                                                MaterialTheme.colorScheme.error
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                )

                                Column(modifier = Modifier.padding(14.dp)) {

                                    // ── Avatar + name + pricing ───────
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar circle with first letter — same as customer card
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                product.name.take(1).uppercase(),
                                                style      = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color      = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                product.name,
                                                style      = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.Sell, null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "₹${product.sellingPrice.formatPrice()}  ·  Cost ₹${product.costPrice.formatPrice()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (product.category.isNotBlank()) {
                                                Row(
                                                    verticalAlignment     = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Filled.Category, null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint     = MaterialTheme.colorScheme.primary)
                                                    Text(product.category,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }

                                        // GST badge — top-right, only when set
                                        if (product.gstPercent > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    "GST ${product.gstPercent.toInt()}%",
                                                    modifier = Modifier.padding(
                                                        horizontal = 7.dp, vertical = 3.dp),
                                                    style  = MaterialTheme.typography.labelSmall,
                                                    color  = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // ── Stock badge — mirrors balance badge ───
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isLowStock)
                                                MaterialTheme.colorScheme.errorContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    if (isLowStock) Icons.Filled.Warning
                                                    else Icons.Filled.Inventory,
                                                    null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint     = if (isLowStock)
                                                        MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    if (isLowStock)
                                                        "Low: ${product.quantity} ${product.unit}"
                                                    else
                                                        "Stock: ${product.quantity} ${product.unit}",
                                                    style      = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color      = if (isLowStock)
                                                        MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Min stock level hint
                                        Text(
                                            "Min: ${product.minStockLevel} ${product.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color     = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    // ── Action row — Edit / Restock / Delete only ──
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        ListActionButton(
                                            icon  = Icons.Filled.Edit,
                                            label = "Edit",
                                            tint  = MaterialTheme.colorScheme.primary
                                        ) { navController.navigate("edit_product/${product.id}") }

                                        ListActionButton(
                                            icon  = Icons.Filled.AddBox,
                                            label = "Restock",
                                            tint  = MaterialTheme.colorScheme.secondary
                                        ) { restockTarget = product; restockQty = "" }

                                        ListActionButton(
                                            icon  = Icons.Filled.DeleteOutline,
                                            label = "Delete",
                                            tint  = MaterialTheme.colorScheme.error
                                        ) {
                                            deleteTargetId   = product.id
                                            deleteTargetName = product.name
                                        }
                                    }
                                }
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

    // ── Delete dialog ─────────────────────────────────────────────────
    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            icon    = { Icon(Icons.Filled.Warning, null,
                tint = MaterialTheme.colorScheme.error) },
            title   = { Text("Delete product") },
            text    = { Text("Delete \"$deleteTargetName\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(deleteTargetId!!) {}
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTargetId = null }) { Text("Cancel") }
            }
        )
    }

    // ── Restock dialog ────────────────────────────────────────────────
    restockTarget?.let { product ->
        AlertDialog(
            onDismissRequest = { restockTarget = null },
            icon    = { Icon(Icons.Filled.AddBox, null,
                tint = MaterialTheme.colorScheme.primary) },
            title   = { Text("Restock") },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("Current: ${product.quantity} ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = restockQty,
                        onValueChange = { restockQty = it.filter { c -> c.isDigit() } },
                        label         = { Text("Quantity to add") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        val qty = restockQty.toIntOrNull() ?: 0
                        if (qty > 0) {
                            viewModel.restockProduct(product.id, qty) {}
                            restockTarget = null
                        }
                    },
                    enabled = restockQty.toIntOrNull()?.let { it > 0 } == true
                ) { Text("Add stock") }
            },
            dismissButton = {
                OutlinedButton(onClick = { restockTarget = null }) { Text("Cancel") }
            }
        )
    }
}