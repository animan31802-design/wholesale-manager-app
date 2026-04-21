package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.animan.wholesalemanager.utils.AppPreferences
import com.google.firebase.auth.FirebaseAuth

data class DrawerItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val badgeCount: Int = 0
)

@Composable
fun AppDrawer(
    navController: NavController,
    onItemClick: () -> Unit,
    lowStockCount: Int = 0
) {
    val context = LocalContext.current
    val shopName = AppPreferences.getShopName(context)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val drawerItems = listOf(
        DrawerItem("Dashboard",     Icons.Filled.Dashboard,    "dashboard"),
        DrawerItem("Create bill",   Icons.Filled.Receipt,      "customer_list"),
        DrawerItem("Bill history",  Icons.Filled.History,      "bill_history"),
        DrawerItem("Customers",     Icons.Filled.People,       "customer_list"),
        DrawerItem("Products",      Icons.Filled.Inventory,    "product_list",
            badgeCount = lowStockCount),
        DrawerItem("Stock consumption",   Icons.Filled.RemoveShoppingCart, "stock_consumption"),
        DrawerItem("Expenses",      Icons.Filled.MoneyOff,     "expenses"),
        DrawerItem("Reports",       Icons.Filled.BarChart,     "reports"),
        DrawerItem("Settings",      Icons.Filled.Settings,     "settings"),
    )

    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {

        // ── Header ────────────────────────────────────────────────────
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
                    shopName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    FirebaseAuth.getInstance().currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Nav items ─────────────────────────────────────────────────
        drawerItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = {
                    if (item.badgeCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text(item.badgeCount.toString()) }
                        }) {
                            Icon(item.icon, contentDescription = null)
                        }
                    } else {
                        Icon(item.icon, contentDescription = null)
                    }
                },
                selected = isSelected,
                onClick = {
                    onItemClick()
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            // Avoid building up a huge back stack
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // ── Logout ────────────────────────────────────────────────────
        NavigationDrawerItem(
            label = { Text("Logout", color = MaterialTheme.colorScheme.error) },
            icon = {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            selected = false,
            onClick = {
                onItemClick()
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}