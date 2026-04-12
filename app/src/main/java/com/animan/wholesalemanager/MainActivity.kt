package com.animan.wholesalemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.*
import com.animan.wholesalemanager.ui.screens.AddCustomerScreen
import com.animan.wholesalemanager.ui.screens.AddProductScreen
import com.animan.wholesalemanager.ui.screens.BillingWrapperScreen
import com.animan.wholesalemanager.ui.screens.CustomerListScreen
import com.animan.wholesalemanager.ui.screens.DashboardScreen
import com.animan.wholesalemanager.ui.screens.LoginScreen
import com.animan.wholesalemanager.ui.screens.ProductListScreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.ui.screens.BillHistoryScreen
import com.animan.wholesalemanager.ui.screens.LedgerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermission()

        setContent {

            val navController = rememberNavController()

            val isLoggedIn = com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .currentUser != null

            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) "dashboard" else "login"
            ) {

                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    DashboardScreen(navController)
                }

                composable("add_customer") {
                    AddCustomerScreen(
                        onCustomerAdded = {
                            navController.popBackStack() // go back to dashboard
                        }
                    )
                }

                composable("customer_list") {
                    CustomerListScreen(navController)
                }

                composable("billing/{customerId}") { backStackEntry ->

                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""

                    BillingWrapperScreen(
                        customerId = customerId,
                        navController = navController
                    )
                }

                composable("product_list") {
                    ProductListScreen(navController)
                }

                composable("add_product") {
                    AddProductScreen(
                        onProductAdded = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("bill_history") {
                    BillHistoryScreen(navController)
                }

                composable("ledger/{customerId}") { backStackEntry ->
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""

                    LedgerScreen(
                        customer = Customer(id = customerId, name = "")
                    )
                }
            }
        }
    }

    fun requestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
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