package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.viewmodel.ProductViewModel

@Composable
fun AddProductScreen(onProductAdded: () -> Unit = {}) {

    val viewModel: ProductViewModel = viewModel()

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Add Product", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = stock,
            onValueChange = { stock = it },
            label = { Text("Stock") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.addProduct(
                    name,
                    price.toDoubleOrNull() ?: 0.0,
                    stock.toIntOrNull() ?: 0
                ) {
                    onProductAdded()
                }
            }
        ) {
            Text("Save Product")
        }
    }
}