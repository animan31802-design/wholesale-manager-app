package com.animan.wholesalemanager.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.animan.wholesalemanager.data.local.Expense
import com.animan.wholesalemanager.repository.ExpenseRepository

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ExpenseRepository(app.applicationContext)

    var expenseList = mutableStateOf<List<Expense>>(emptyList())
    var totalExpense = mutableStateOf(0.0)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun fetchExpenses() {
        isLoading.value = true
        repository.getExpenses(
            onResult = {
                expenseList.value = it
                totalExpense.value = it.sumOf { e -> e.amount }
                isLoading.value = false
            },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    fun addExpense(title: String, amount: Double, onSuccess: () -> Unit) {
        if (title.isBlank()) { errorMessage.value = "Title cannot be empty"; return }
        if (amount <= 0) { errorMessage.value = "Amount must be greater than 0"; return }
        repository.addExpense(
            Expense(title = title.trim(), amount = amount),
            onSuccess = { fetchExpenses(); onSuccess() },
            onError = { errorMessage.value = it }
        )
    }

    fun deleteExpense(id: String) {
        repository.deleteExpense(id,
            onSuccess = { fetchExpenses() },
            onError = { errorMessage.value = it }
        )
    }
}