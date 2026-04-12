package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.animan.wholesalemanager.printer.PrinterManager
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Create Bill (go to customer list → select customer → billing)
        Button(
            onClick = {
                navController.navigate("customer_list")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Bill")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                navController.navigate("bill_history")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bill History")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 👥 Customers
        Button(
            onClick = {
                navController.navigate("customer_list")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Customers")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Products
        Button(
            onClick = {
                navController.navigate("product_list")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Products")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Divider()

        Spacer(modifier = Modifier.height(20.dp))

        val printerManager = remember { PrinterManager() }
        var message by remember { mutableStateOf("") }

        Spacer(modifier = Modifier.height(20.dp))

        Divider()

        Spacer(modifier = Modifier.height(20.dp))

        val context = LocalContext.current
        // Test Print
        Button(
            onClick = {
                val result = printerManager.printTestBill(context)
                message = result
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Print")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Show message
        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("dashboard") { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Logout")
        }
    }
}