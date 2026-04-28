package com.animan.wholesalemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animan.wholesalemanager.ui.screens.*
import com.animan.wholesalemanager.ui.theme.WholesaleManagerTheme
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.work.BackupScheduler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermission()

        AppLanguage.load(this)
        BackupScheduler.schedule(this)

        setContent {
            WholesaleManagerTheme {
                val navController = rememberNavController()
                val isLoggedIn    = com.google.firebase.auth.FirebaseAuth
                    .getInstance().currentUser != null

                val customerViewModel: com.animan.wholesalemanager.viewmodel.CustomerViewModel = viewModel()
                val supplierViewModel: com.animan.wholesalemanager.viewmodel.SupplierViewModel = viewModel()

                NavHost(
                    navController    = navController,
                    startDestination = if (isLoggedIn) "dashboard" else "login"
                ) {

                    // ── Auth ──────────────────────────────────────────
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess       = {
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() }
                        )
                    }

                    // ── Main screens ──────────────────────────────────
                    composable("dashboard")     { DashboardScreen(navController) }
                    composable("customer_list") { CustomerListScreen(navController) }
                    composable("product_list")  { ProductListScreen(navController) }
                    composable("expenses")      { ExpenseScreen() }
                    composable("reports")       { ReportScreen() }
                    composable("bill_history")  { BillHistoryScreen(navController) }
                    composable("settings")      { SettingsScreen(navController) }
                    composable("stock_consumption") { StockConsumptionScreen(navController) }
                    composable("printer_selector")  { PrinterSelectorScreen(navController) }
                    composable("location_picker")   { LocationPickerScreen(navController) }
                    composable("about")             { AboutScreen(navController) }

                    // ── Billing ───────────────────────────────────────
                    composable("billing/{customerId}") { backStack ->
                        BillingWrapperScreen(
                            backStack.arguments?.getString("customerId") ?: "",
                            navController
                        )
                    }

                    composable("payment/{customerId}") { backStack ->
                        PaymentEntryScreen(
                            backStack.arguments?.getString("customerId") ?: "",
                            navController
                        )
                    }

                    composable("ledger/{customerId}") { backStack ->
                        LedgerWrapperScreen(
                            backStack.arguments?.getString("customerId") ?: "",
                            navController
                        )
                    }

                    // ── Products ──────────────────────────────────────
                    composable("add_product") { AddProductScreen(navController) }

                    composable(
                        route     = "edit_product/{productId}",
                        arguments = listOf(navArgument("productId") {
                            type = NavType.StringType })
                    ) { backStack ->
                        AddProductScreen(
                            navController,
                            backStack.arguments?.getString("productId")
                        )
                    }

                    // ── Add Customer ──────────────────────────────────
                    composable("add_customer") {
                        AddContactScreen(
                            title         = "Add customer",
                            navController = navController,
                            showLocation  = true,
                            isLoading     = customerViewModel.isLoading.value,
                            errorMessage  = customerViewModel.errorMessage.value,
                            onSave        = { name, phone, address, lat, lng ->
                                customerViewModel.addCustomer(
                                    name, phone, address, lat, lng
                                ) { navController.popBackStack() }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── Edit Customer ─────────────────────────────────
                    // FIX: fetch customers first if list is empty, then
                    // find and render — avoids silent null when navigating
                    // directly to this route before list loads
                    composable(
                        route     = "edit_customer/{customerId}",
                        arguments = listOf(navArgument("customerId") {
                            type = NavType.StringType })
                    ) { backStack ->
                        val customerId = backStack.arguments?.getString("customerId")
                            ?: return@composable

                        // Ensure customer list is loaded
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (customerViewModel.customerList.value.isEmpty()) {
                                customerViewModel.fetchCustomers()
                            }
                        }

                        val customer = customerViewModel.customerList.value
                            .find { it.id == customerId }

                        if (customer != null) {
                            AddContactScreen(
                                title         = "Edit customer",
                                navController = navController,
                                initialName   = customer.name,
                                initialPhone  = customer.phone,
                                initialAddr   = customer.address,
                                initialLat    = customer.latitude,
                                initialLng    = customer.longitude,
                                showLocation  = true,
                                isLoading     = customerViewModel.isLoading.value,
                                errorMessage  = customerViewModel.errorMessage.value,
                                onSave        = { name, phone, address, lat, lng ->
                                    customerViewModel.updateCustomer(
                                        customer.copy(
                                            name      = name,
                                            phone     = phone,
                                            address   = address,
                                            latitude  = lat,
                                            longitude = lng
                                        )
                                    ) { navController.popBackStack() }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            // Still loading — show spinner
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    }

                    // ── Add Supplier ──────────────────────────────────
                    composable("add_supplier") {
                        AddContactScreen(
                            title         = "Add supplier",
                            navController = navController,
                            showLocation  = true,
                            isLoading     = supplierViewModel.isLoading.value,
                            errorMessage  = supplierViewModel.errorMessage.value,
                            onSave        = { name, phone, address, lat, lng ->
                                supplierViewModel.addSupplier(
                                    name, phone, address, lat, lng
                                ) { navController.popBackStack() }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ── Edit Supplier ─────────────────────────────────
                    composable(
                        route     = "edit_supplier/{supplierId}",
                        arguments = listOf(navArgument("supplierId") {
                            type = NavType.StringType })
                    ) { backStack ->
                        val supplierId = backStack.arguments?.getString("supplierId")
                            ?: return@composable

                        // Ensure supplier list is loaded
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (supplierViewModel.supplierList.value.isEmpty()) {
                                supplierViewModel.fetchSuppliers()
                            }
                        }

                        val supplier = supplierViewModel.supplierList.value
                            .find { it.id == supplierId }

                        if (supplier != null) {
                            AddContactScreen(
                                title         = "Edit supplier",
                                navController = navController,
                                initialName   = supplier.name,
                                initialPhone  = supplier.phone,
                                initialAddr   = supplier.address,
                                initialLat    = supplier.latitude,
                                initialLng    = supplier.longitude,
                                showLocation  = true,
                                isLoading     = supplierViewModel.isLoading.value,
                                errorMessage  = supplierViewModel.errorMessage.value,
                                onSave        = { name, phone, address, lat, lng ->
                                    supplierViewModel.updateSupplier(
                                        supplier.copy(
                                            name      = name,
                                            phone     = phone,
                                            address   = address,
                                            latitude  = lat,
                                            longitude = lng
                                        )
                                    ) { navController.popBackStack() }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    }

                    // ── Supplier list ─────────────────────────────────
                    composable("supplier_list") {
                        SupplierListScreen(navController = navController)
                    }

                    // ── Purchase ──────────────────────────────────────
                    composable(
                        route     = "purchase/{supplierId}?preselect={productId}",
                        arguments = listOf(
                            navArgument("supplierId") { type = NavType.StringType },
                            navArgument("productId")  {
                                type          = NavType.StringType
                                nullable      = true
                                defaultValue  = null
                            }
                        )
                    ) { backStack ->
                        val supplierId = backStack.arguments?.getString("supplierId") ?: return@composable
                        val preselect  = backStack.arguments?.getString("productId")  // nullable — null if not passed
                        PurchaseScreen(
                            supplierId         = supplierId,
                            preselectProductId = preselect,
                            onPurchaseSaved    = { navController.popBackStack() },
                            onBack             = { navController.popBackStack() }
                        )
                    }

                    // ── Purchase history ──────────────────────────────
                    composable("purchase_history") {
                        PurchaseHistoryScreen(navController = navController)
                    }

                    // ── Supplier detail ───────────────────────────────
                    composable(
                        route     = "supplier_detail/{supplierId}",
                        arguments = listOf(navArgument("supplierId") {
                            type = NavType.StringType })
                    ) { backStack ->
                        val supplierId = backStack.arguments?.getString("supplierId")
                            ?: return@composable
                        SupplierDetailScreen(
                            supplierId    = supplierId,
                            navController = navController
                        )
                    }

                    // ── Supplier report ───────────────────────────────
                    composable("supplier_report") {
                        SupplierReportScreen(navController = navController)
                    }
                }
            }
        }
    }

    private fun requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
        }
    }
}