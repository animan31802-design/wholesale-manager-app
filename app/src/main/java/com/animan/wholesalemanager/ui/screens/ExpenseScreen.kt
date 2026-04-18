package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animan.wholesalemanager.viewmodel.ExpenseViewModel

@Composable
fun ExpenseScreen() {

    val viewModel: ExpenseViewModel = viewModel()

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchExpenses()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Expenses", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Expense Name") }
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = {
            viewModel.addExpense(
                title,
                amount.toDoubleOrNull() ?: 0.0
            ) {
                title = ""
                amount = ""
                viewModel.fetchExpenses()
            }
        }) {
            Text("Add Expense")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Total Expense: ₹${viewModel.totalExpense.value}")

        LazyColumn {
            items(viewModel.expenseList.value) { expense ->
                Text("${expense.title} - ₹${expense.amount}")
            }
        }
    }
}