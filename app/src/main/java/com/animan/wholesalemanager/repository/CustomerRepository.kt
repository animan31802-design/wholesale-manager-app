package com.animan.wholesalemanager.repository

import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.Expense
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
        onSuccess: (String) -> Unit,
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
            .addOnSuccessListener { onSuccess(id) }
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
        items: List<com.animan.wholesalemanager.data.local.BillItem>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        val batch = db.batch()

        try {
            items.forEach { item ->

                val productRef = db.collection("users")
                    .document(userId!!)
                    .collection("products")
                    .document(item.productId)

                batch.update(productRef, "quantity", com.google.firebase.firestore.FieldValue.increment(-item.quantity.toLong()))
            }

            batch.commit()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onError(it.message ?: "Stock update failed") }

        } catch (e: Exception) {
            onError(e.message ?: "Error")
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

    fun getBillsSummary(
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

    fun addExpense(
        expense: Expense,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
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
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .document(userId!!)
            .collection("expenses")
            .get()
            .addOnSuccessListener {
                val list = it.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)
                }
                onResult(list)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error")
            }
    }
}