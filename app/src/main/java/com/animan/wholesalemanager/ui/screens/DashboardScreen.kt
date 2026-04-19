package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.PrinterManager
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.utils.LOW_STOCK_THRESHOLD
import com.animan.wholesalemanager.viewmodel.BillViewModel
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val productViewModel: ProductViewModel = viewModel()

    val lowStockCount = productViewModel.productList.value
        .count { it.quantity <= LOW_STOCK_THRESHOLD }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                navController = navController,
                onItemClick = { scope.launch { drawerState.close() } },
                lowStockCount = lowStockCount
            )
        }
    )  {

        Scaffold(
            bottomBar = { BottomNavBar(navController) },
            topBar = {
                TopAppBar(
                    title = { Text("Wholesale Manager") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->

            // 👉 KEEP YOUR EXISTING CONTENT
            DashboardContent(navController, padding)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title)
            Text(title)
        }
    }
}

@Composable
fun HighlightCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun QuickActions(navController: NavController) {

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionItem("Bill", Icons.Default.Add, Modifier.weight(1f)) {
                navController.navigate("customer_list")
            }
            ActionItem("Customers", Icons.Default.Person, Modifier.weight(1f)) {
                navController.navigate("customer_list")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionItem("Products", Icons.Default.Inventory, Modifier.weight(1f)) {
                navController.navigate("product_list")
            }
            ActionItem("Expenses", Icons.Default.Money, Modifier.weight(1f)) {
                navController.navigate("expenses")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionItem("Reports", Icons.Default.BarChart, Modifier.weight(1f)) {
                navController.navigate("reports")
            }
            ActionItem("History", Icons.Default.List, Modifier.weight(1f)) {
                navController.navigate("bill_history")
            }
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title)
            Text(title)
        }
    }
}

@Composable
fun AlertCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "⚠ $text",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun BottomNavBar(navController: NavController) {

    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("reports") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
            label = { Text("Reports") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("product_list") },
            icon = { Icon(Icons.Default.Inventory, contentDescription = "Products") },
            label = { Text("Products") }
        )
    }
}

@Composable
fun DashboardContent(navController: NavController, padding: PaddingValues) {

    val billViewModel: BillViewModel = viewModel()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()

    LaunchedEffect(Unit) {
        billViewModel.fetchBills()
    }

    val totalSales = billViewModel.billList.value.sumOf { it.itemsTotal }
    val totalExpenses = expenseViewModel.expenseList.value.sumOf { it.amount }
    val profit = totalSales - totalExpenses

    val lowStockProducts = productViewModel.productList.value
        .filter { it.quantity <= LOW_STOCK_THRESHOLD }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {

        Text("Welcome back 👋", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HighlightCard("Sales", "₹$totalSales")
            HighlightCard("Expenses", "₹$totalExpenses")
            HighlightCard("Profit", "₹$profit")
        }

        Spacer(Modifier.height(20.dp))

        Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(10.dp))

        QuickActions(navController)

        Spacer(Modifier.height(20.dp))

        if (lowStockProducts.isNotEmpty()) {
            AlertCard("${lowStockProducts.size} items low in stock")
        }
    }
}

@Composable
fun AppDrawer(
    navController: NavController,
    onItemClick: () -> Unit,
    lowStockCount: Int
) {

    ModalDrawerSheet {

        // 🌈 HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.Bottom) {
                Text(
                    "Wholesale Manager",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Owner: Animan",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 🧭 MAIN
        DrawerItem("Dashboard", Icons.Default.Home) {
            navController.navigate("dashboard")
            onItemClick()
        }

        DrawerItem("Customers", Icons.Default.Person) {
            navController.navigate("customer_list")
            onItemClick()
        }

        DrawerItem("Products", Icons.Default.Inventory) {
            navController.navigate("product_list")
            onItemClick()
        }

        DrawerItemWithBadge(
            "Low Stock",
            Icons.Default.Warning,
            lowStockCount
        ) {
            navController.navigate("product_list")
            onItemClick()
        }

        DrawerItem("Reports", Icons.Default.BarChart) {
            navController.navigate("reports")
            onItemClick()
        }

        Divider(Modifier.padding(vertical = 8.dp))

        Text(
            "Settings",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.labelMedium
        )

        DrawerItem("Backup", Icons.Default.Cloud) {
            navController.navigate("backup_settings")
            onItemClick()
        }

        DrawerItem("Printer Setup", Icons.Default.Print) {
            onItemClick()
        }

        Spacer(Modifier.weight(1f))

        Divider()

        DrawerItem("Logout", Icons.Default.ExitToApp) {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo("dashboard") { inclusive = true }
            }
            onItemClick()
        }
    }
}

@Composable
fun DrawerItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title)
                Spacer(Modifier.width(12.dp))
                Text(title)
            }
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun DrawerItemWithBadge(
    title: String,
    icon: ImageVector,
    badgeCount: Int,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = title)
                Spacer(Modifier.width(12.dp))
                Text(title)

                Spacer(Modifier.weight(1f))

                if (badgeCount > 0) {
                    Badge {
                        Text(badgeCount.toString())
                    }
                }
            }
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}