package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Supplier
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(navController: NavController) {

    val viewModel: SupplierViewModel = viewModel()
    var searchQuery    by remember { mutableStateOf("") }
    var deleteSupplier by remember { mutableStateOf<Supplier?>(null) }

    LaunchedEffect(Unit) { viewModel.fetchSuppliers() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) viewModel.fetchSuppliers()
        else viewModel.searchSuppliers(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suppliers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("purchase_history") }) {
                        Icon(Icons.Filled.History, contentDescription = "Purchase history")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_supplier") },  // ← navigate, not dialog
                icon    = { Icon(Icons.Filled.Add, null) },
                text    = { Text("Add supplier") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                label         = { Text("Search suppliers…") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                leadingIcon   = { Icon(Icons.Filled.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotEmpty())
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null)
                        }
                }
            )

            Spacer(Modifier.height(12.dp))

            when {
                viewModel.isLoading.value ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                viewModel.supplierList.value.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.LocalShipping, null,
                                modifier = Modifier.size(48.dp),
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No suppliers yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap + to add your first supplier",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.supplierList.value, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier   = supplier,
                            onClick    = {
                                navController.navigate("supplier_detail/${supplier.id}")
                            },
                            onEdit     = {
                                navController.navigate("edit_supplier/${supplier.id}")  // ← navigate
                            },
                            onDelete   = { deleteSupplier = supplier },
                            onPurchase = {
                                navController.navigate("purchase/${supplier.id}")
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Delete confirm dialog ─────────────────────────────────────────
    deleteSupplier?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteSupplier = null },
            title   = { Text("Delete supplier?") },
            text    = { Text("This will remove ${s.name}. Past purchases will remain.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplier(s.id) { deleteSupplier = null }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteSupplier = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Supplier card ─────────────────────────────────────────────────────
@Composable
private fun SupplierCard(
    supplier   : Supplier,
    onClick    : () -> Unit,
    onEdit     : () -> Unit,
    onDelete   : () -> Unit,
    onPurchase : () -> Unit
) {
    val hasBalance = supplier.balance > 0.001

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(supplier.name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    if (supplier.phone.isNotBlank())
                        Text(supplier.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (supplier.address.isNotBlank())
                        Text(supplier.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (hasBalance) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Due: ${supplier.balance.toRupees()}",
                            modifier   = Modifier.padding(
                                horizontal = 10.dp, vertical = 5.dp),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick  = onPurchase,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ShoppingCart, null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Purchase")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, null,
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null,
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}