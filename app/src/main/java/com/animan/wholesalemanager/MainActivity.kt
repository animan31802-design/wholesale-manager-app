package com.animan.wholesalemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.animan.wholesalemanager.ui.screens.*
import com.animan.wholesalemanager.ui.theme.WholesaleManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetoothPermission()

        setContent {
            WholesaleManagerTheme {
                val navController = rememberNavController()
                val isLoggedIn = com.google.firebase.auth.FirebaseAuth
                    .getInstance().currentUser != null

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) "dashboard" else "login"
                ) {
                    composable("login") {
                        LoginScreen(onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        })
                    }

                    composable("dashboard") { DashboardScreen(navController) }

                    composable("add_customer") {
                        AddCustomerScreen(onCustomerAdded = { navController.popBackStack() })
                    }

                    composable("customer_list") { CustomerListScreen(navController) }

                    composable("billing/{customerId}") { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        BillingWrapperScreen(customerId = customerId, navController = navController)
                    }

                    composable("product_list") { ProductListScreen(navController) }

                    composable("add_product") { AddProductScreen(navController) }

                    composable("edit_product/{productId}") { backStackEntry ->
                        val productId = backStackEntry.arguments?.getString("productId") ?: ""
                        AddProductScreen(navController, productId)
                    }

                    composable("expenses") { ExpenseScreen() }

                    composable("reports") { ReportScreen() }

                    composable("bill_history") { BillHistoryScreen(navController) }

                    // FIX: uses LedgerWrapperScreen — resolves full Customer before rendering
                    composable("ledger/{customerId}") { backStackEntry ->
                        val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                        LedgerWrapperScreen(customerId = customerId, navController = navController)
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