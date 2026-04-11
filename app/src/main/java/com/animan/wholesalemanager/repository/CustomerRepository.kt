package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Customer
import com.google.firebase.firestore.FirebaseFirestore

class CustomerRepository {

    private val db = FirebaseFirestore.getInstance()

    fun addCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = db.collection("customers").document().id
        val newCustomer = customer.copy(id = id)

        db.collection("customers")
            .document(id)
            .set(newCustomer)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }
}