package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Expense
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExpenseRepository {

    private val db = FirebaseFirestore.getInstance()

    fun addExpense(
        expense: Expense,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val id = db.collection("expenses").document().id

        val newExpense = expense.copy(id = id)

        db.collection("users")
            .document(userId!!)
            .collection("expenses")
            .document(id)
            .set(newExpense)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getExpenses(
        onResult: (List<Expense>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("expenses")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    it.toObject(Expense::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }
}