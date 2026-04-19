package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.*
import java.util.UUID

class CustomerRepository(private val context: Context) {

    private val db by lazy { com.animan.wholesalemanager.data.local.DatabaseHelper(context) }

    fun addCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val newCustomer = customer.copy(id = UUID.randomUUID().toString())
        if (db.insertCustomer(newCustomer)) onSuccess()
        else onError("Failed to save customer")
    }

    fun getCustomers(onResult: (List<Customer>) -> Unit, onError: (String) -> Unit) {
        try {
            onResult(db.getAllCustomers())
        } catch (e: Exception) {
            onError(e.message ?: "Error loading customers")
        }
    }

    fun updateCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.updateCustomer(customer)) onSuccess()
        else onError("Failed to update customer")
    }

    fun deleteCustomer(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteCustomer(id)) onSuccess()
        else onError("Failed to delete customer")
    }

    fun searchCustomers(query: String): List<Customer> = db.searchCustomers(query)

    // Saves bill + updates stock + writes two ledger entries + updates customer balance
    // All in one coordinated operation so nothing goes out of sync.
    fun saveBill(
        bill: Bill,
        customer: Customer,
        onSuccess: (billId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val billId = UUID.randomUUID().toString()
        val newBill = bill.copy(id = billId)

        val billSaved = db.insertBillWithItems(newBill)
        if (!billSaved) {
            onError("Failed to save bill")
            return
        }

        // Decrement stock for each item
        newBill.items.forEach { item ->
            db.decrementProductStock(item.productId, item.quantity)
        }

        // CREDIT ledger entry
        db.insertLedgerEntry(
            Ledger(
                id = UUID.randomUUID().toString(),
                customerId = customer.id,
                amount = bill.itemsTotal,
                type = "CREDIT",
                billId = billId,
                note = "Bill #${billId.take(8)}",
                timestamp = System.currentTimeMillis()
            )
        )

        // PAYMENT ledger entry (only if something was paid)
        if (bill.paidAmount > 0) {
            db.insertLedgerEntry(
                Ledger(
                    id = UUID.randomUUID().toString(),
                    customerId = customer.id,
                    amount = bill.paidAmount,
                    type = "PAYMENT",
                    billId = billId,
                    note = "Payment on bill #${billId.take(8)}",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Update customer totals correctly
        val updatedCustomer = customer.copy(
            totalPurchase = customer.totalPurchase + bill.itemsTotal,
            totalPaid = customer.totalPaid + bill.paidAmount,
            balance = customer.balance + bill.itemsTotal - bill.paidAmount
        )
        db.updateCustomer(updatedCustomer)

        onSuccess(billId)
    }

    fun getBills(onResult: (List<Bill>) -> Unit, onError: (String) -> Unit) {
        try {
            onResult(db.getAllBills())
        } catch (e: Exception) {
            onError(e.message ?: "Error loading bills")
        }
    }

    fun getBillsByDateRange(
        from: Long,
        to: Long,
        onResult: (List<Bill>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            onResult(db.getBillsByDateRange(from, to))
        } catch (e: Exception) {
            onError(e.message ?: "Error loading bills")
        }
    }

    // Standalone payment — customer paying off their balance without a new bill
    fun recordPayment(
        customer: Customer,
        amount: Double,
        note: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ledgerSaved = db.insertLedgerEntry(
            Ledger(
                id = UUID.randomUUID().toString(),
                customerId = customer.id,
                amount = amount,
                type = "PAYMENT",
                billId = "",
                note = note.ifBlank { "Standalone payment" },
                timestamp = System.currentTimeMillis()
            )
        )
        if (!ledgerSaved) {
            onError("Failed to record payment")
            return
        }
        val updatedCustomer = customer.copy(
            totalPaid = customer.totalPaid + amount,
            balance = customer.balance - amount
        )
        db.updateCustomer(updatedCustomer)
        onSuccess()
    }

    fun getLedger(
        customerId: String,
        onResult: (List<Ledger>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            onResult(db.getLedgerByCustomer(customerId))
        } catch (e: Exception) {
            onError(e.message ?: "Error loading ledger")
        }
    }

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
}