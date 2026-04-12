package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.viewmodel.ProductViewModel

@Composable
fun ProductListScreen(
    navController: NavController
) {

    val viewModel: ProductViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchProducts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Products",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                navController.navigate("add_product")
            }
        ) {
            Text("Add Product")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (viewModel.isLoading.value) {
            CircularProgressIndicator()
        }

        LazyColumn {

            items(viewModel.productList.value) { product ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {

                    Column(modifier = Modifier.padding(10.dp)) {

                        Text("Name: ${product.name}")
                        Text("Price: ₹${product.price}")
                        Text("Stock: ${product.stock}")
                    }
                }
            }
        }

        viewModel.errorMessage.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}