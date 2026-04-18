package com.animan.wholesalemanager.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.animan.wholesalemanager.data.local.Expense
import com.animan.wholesalemanager.repository.CustomerRepository
import com.animan.wholesalemanager.repository.ExpenseRepository

class ExpenseViewModel : ViewModel() {

    private val repository = CustomerRepository()

    var expenseList = mutableStateOf<List<Expense>>(emptyList())
    var totalExpense = mutableStateOf(0.0)

    fun fetchExpenses() {
        repository.getExpenses(
            onResult = {
                expenseList.value = it
                totalExpense.value = it.sumOf { e -> e.amount }
            },
            onError = {}
        )
    }

    fun addExpense(title: String, amount: Double, onSuccess: () -> Unit) {

        val expense = Expense(
            title = title,
            amount = amount
        )

        repository.addExpense(
            expense,
            onSuccess = onSuccess,
            onError = {}
        )
    }
}