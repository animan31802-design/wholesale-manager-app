package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen() {

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

        Button(onClick = { }) {
            Text("Create Bill")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { }) {
            Text("Customers")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { }) {
            Text("Products")
        }
    }
}