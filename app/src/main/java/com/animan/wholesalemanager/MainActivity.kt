package com.animan.wholesalemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.*
import com.animan.wholesalemanager.ui.screens.AddCustomerScreen
import com.animan.wholesalemanager.ui.screens.DashboardScreen
import com.animan.wholesalemanager.ui.screens.LoginScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            }
        }
    }
}