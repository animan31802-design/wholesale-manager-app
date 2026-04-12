package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.Customer
import com.google.firebase.firestore.FirebaseFirestore

class CustomerRepository {

    private val db = FirebaseFirestore.getInstance()

    fun addCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val id = db.collection("customers").document().id
        val newCustomer = customer.copy(id = id)

        db.collection("users")
            .document(userId!!)
            .collection("customers")
            .document(id)
            .set(newCustomer)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getCustomers(
        onResult: (List<Customer>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("customers")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    it.toObject(Customer::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }

    fun saveBill(
        bill: Bill,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        val id = db.collection("bills").document().id
        val newBill = bill.copy(id = id)

        db.collection("users")
            .document(userId!!)
            .collection("bills")
            .document(id)
            .set(newBill)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getBills(
        onResult: (List<Bill>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("bills")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    it.toObject(Bill::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }
}