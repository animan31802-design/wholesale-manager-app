package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.data.local.Expense
import java.util.UUID

class ExpenseRepository(private val context: Context) {

    private val db by lazy { DatabaseHelper(context) }

    fun addExpense(expense: Expense, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newExpense = expense.copy(id = UUID.randomUUID().toString())
        if (db.insertExpense(newExpense)) onSuccess()
        else onError("Failed to save expense")
    }

    fun getExpenses(onResult: (List<Expense>) -> Unit, onError: (String) -> Unit) {
        try {
            onResult(db.getAllExpenses())
        } catch (e: Exception) {
            onError(e.message ?: "Error loading expenses")
        }
    }

    fun deleteExpense(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteExpense(id)) onSuccess()
        else onError("Failed to delete expense")
    }

    fun getExpensesByDateRange(
        from: Long,
        to: Long,
        onResult: (List<Expense>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            onResult(db.getExpensesByDateRange(from, to))
        } catch (e: Exception) {
            onError(e.message ?: "Error loading expenses")
        }
    }
}