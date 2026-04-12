package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.Ledger
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

    fun updateProductStock(
        items: List<com.animan.wholesalemanager.data.local.BillItem>
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        items.forEach { item ->

            val productRef = db.collection("users")
                .document(userId!!)
                .collection("products")
                .document(item.productId)

            db.runTransaction { transaction ->

                val snapshot = transaction.get(productRef)
                val currentQty = snapshot.getLong("quantity")?.toInt() ?: 0

                val newQty = (currentQty - item.quantity).coerceAtLeast(0)

                transaction.update(productRef, "quantity", newQty)

            }
        }
    }

    fun addLedgerEntry(
        ledger: Ledger,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val id = db.collection("ledger").document().id
        val newLedger = ledger.copy(id = id)

        db.collection("users")
            .document(userId!!)
            .collection("ledger")
            .document(id)
            .set(newLedger)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error") }
    }

    fun getLedger(
        customerId: String,
        onResult: (List<Ledger>) -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("ledger")
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    it.toObject(Ledger::class.java)
                }.sortedByDescending { it.timestamp }

                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }
}