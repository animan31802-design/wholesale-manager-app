package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.viewmodel.ProductViewModel

@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null
) {

    val viewModel: ProductViewModel = viewModel()

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
    }

    LaunchedEffect(productId) {
        if (productId != null) {
            val product = viewModel.productList.value.find { it.id == productId }
            product?.let {
                name = it.name
                price = it.price.toString()
                quantity = it.quantity.toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            if (productId == null) "Add Product" else "Edit Product",
            style = MaterialTheme.typography.headlineMedium
        )

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
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val product = Product(
                    id = productId ?: "",
                    name = name,
                    price = price.toDoubleOrNull() ?: 0.0,
                    quantity = quantity.toIntOrNull() ?: 0
                )

                if (productId == null) {
                    viewModel.addProduct(
                        name,
                        product.price,
                        product.quantity
                    ) {
                        navController.popBackStack()
                    }
                } else {
                    viewModel.updateProduct(product) {
                        navController.popBackStack()
                    }
                }
            }
        ) {
            Text(if (productId == null) "Add" else "Update")
        }
    }
}