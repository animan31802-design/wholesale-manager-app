package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.PriceUtils.toRupees
import com.animan.wholesalemanager.viewmodel.AuthViewModel
import com.animan.wholesalemanager.viewmodel.SupplierViewModel
import com.google.firebase.auth.FirebaseAuth

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0,
    val badgeText: String? = null
)

@Composable
fun AppDrawer(
    navController: NavController,
    onItemClick: () -> Unit,
    lowStockCount: Int = 0
) {
    val authViewModel: AuthViewModel = viewModel()
    val supplierViewModel: SupplierViewModel = viewModel()

    val context = LocalContext.current
    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    var showSupplierPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        supplierViewModel.fetchSuppliers()
    }

    val suppliersDueCount =
        supplierViewModel.supplierList.value.count { it.balance > 0.001 }

    val drawerItems = listOf(
        DrawerItem("Dashboard", Icons.Filled.Dashboard, "dashboard"),
        DrawerItem("Create bill", Icons.Filled.Receipt, "customer_list"),
        DrawerItem("Bill history", Icons.Filled.History, "bill_history"),
        DrawerItem("Customers", Icons.Filled.People, "customer_list"),
        DrawerItem(
            "Products",
            Icons.Filled.Inventory,
            "product_list",
            badgeCount = lowStockCount
        ),
        DrawerItem(
            "Stock consumption",
            Icons.Filled.RemoveShoppingCart,
            "stock_consumption"
        ),
        DrawerItem(
            "Suppliers & Purchase",
            Icons.Filled.LocalShipping,
            "supplier_list",
            badgeCount = suppliersDueCount
        ),
        DrawerItem("Expenses", Icons.Filled.MoneyOff, "expenses"),
        DrawerItem("Reports", Icons.Filled.BarChart, "reports"),
        DrawerItem("About", Icons.Filled.Info, "about"),
        DrawerItem("Settings", Icons.Filled.Settings, "settings")
    )

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // Fixed Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(24.dp)
            ) {
                Column {
                    Icon(
                        Icons.Filled.Store,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = AppPreferences.getShopName(context),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = FirebaseAuth.getInstance().currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                drawerItems.forEach { item ->

                    val isSelected = currentRoute == item.route

                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        icon = {
                            if (item.badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(item.badgeCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null
                                    )
                                }
                            } else {
                                Icon(
                                    item.icon,
                                    contentDescription = null
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            onItemClick()

                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 4.dp
                    )
                )

                // New Purchase
                NavigationDrawerItem(
                    label = {
                        Text(
                            "New purchase",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Filled.AddShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    selected = false,
                    onClick = {
                        onItemClick()
                        supplierViewModel.fetchSuppliers()
                        showSupplierPicker = true
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Logout
                NavigationDrawerItem(
                    label = {
                        if (authViewModel.isLoading.value) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Text(
                                    "Backing up…",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                        } else {
                            Text(
                                "Logout",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    selected = false,
                    onClick = {
                        if (authViewModel.isLoading.value) return@NavigationDrawerItem

                        onItemClick()

                        authViewModel.logoutWithBackup(context) {
                            navController.navigate("login") {
                                popUpTo(0) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Supplier Picker Dialog
        if (showSupplierPicker) {
            SupplierPickerDialog(
                suppliers = supplierViewModel.supplierList.value,
                onSelect = { supplier ->
                    showSupplierPicker = false
                    navController.navigate("purchase/${supplier.id}")
                },
                onDismiss = {
                    showSupplierPicker = false
                }
            )
        }
    }
}

// ── Supplier picker dialog ────────────────────────────────────────────
@Composable
private fun SupplierPickerDialog(
    suppliers : List<com.animan.wholesalemanager.data.local.Supplier>,
    onSelect  : (com.animan.wholesalemanager.data.local.Supplier) -> Unit,
    onDismiss : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Select supplier") },
        text    = {
            if (suppliers.isEmpty()) {
                Text("No suppliers found. Add a supplier first from Suppliers & Purchase.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suppliers.forEach { supplier ->
                        Card(
                            onClick  = { onSelect(supplier) },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(supplier.name,
                                        style = MaterialTheme.typography.titleSmall)
                                    if (supplier.phone.isNotBlank())
                                        Text(supplier.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (supplier.balance > 0) {
                                    Text("Due: ${supplier.balance.toRupees()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
