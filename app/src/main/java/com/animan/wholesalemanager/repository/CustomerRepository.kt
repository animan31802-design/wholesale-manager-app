package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.*
import java.util.UUID

class CustomerRepository(private val context: Context) {

    private val db by lazy { DatabaseHelper(context) }

    fun addCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val new = customer.copy(id = UUID.randomUUID().toString())
        if (db.insertCustomer(new)) onSuccess() else onError("Failed to save customer")
    }

    fun getCustomers(onResult: (List<Customer>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllCustomers()) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun updateCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.updateCustomer(customer)) onSuccess() else onError("Failed to update")
    }

    fun deleteCustomer(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteCustomer(id)) onSuccess() else onError("Failed to delete")
    }

    fun searchCustomers(query: String): List<Customer> = db.searchCustomers(query)

    // ── Save bill (GST-aware) ─────────────────────────────────────────
    fun saveBill(
        bill: Bill,
        customer: Customer,
        onSuccess: (billId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val billId   = UUID.randomUUID().toString()
        val newBill  = bill.copy(id = billId)

        if (!db.insertBillWithItems(newBill)) { onError("Failed to save bill"); return }

        newBill.items.forEach { db.decrementProductStock(it.productId, it.quantity) }

        db.insertLedgerEntry(Ledger(
            id = UUID.randomUUID().toString(), customerId = customer.id,
            amount = bill.grandTotal, type = "CREDIT",
            billId = billId, note = "Bill #${billId.take(8)}",
            timestamp = System.currentTimeMillis()
        ))

        if (bill.paidAmount > 0) {
            db.insertLedgerEntry(Ledger(
                id = UUID.randomUUID().toString(), customerId = customer.id,
                amount = bill.paidAmount, type = "PAYMENT",
                billId = billId, note = "Payment on bill #${billId.take(8)}",
                timestamp = System.currentTimeMillis()
            ))
        }

        db.updateCustomer(customer.copy(
            totalPurchase = customer.totalPurchase + bill.grandTotal,
            totalPaid     = customer.totalPaid + bill.paidAmount,
            balance       = customer.balance + bill.grandTotal - bill.paidAmount
        ))

        onSuccess(billId)
    }

    // ── Refund a bill ─────────────────────────────────────────────────
    // Marks bill as refunded, restocks all items, reverses customer balance
    fun refundBill(
        bill: Bill,
        customer: Customer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (bill.isRefunded) { onError("This bill has already been refunded"); return }

        // Restock every item
        bill.items.forEach { db.incrementProductStock(it.productId, it.quantity) }

        // Mark bill refunded
        db.markBillRefunded(bill.id)

        // Reverse ledger: REFUND entry
        db.insertLedgerEntry(Ledger(
            id = UUID.randomUUID().toString(), customerId = customer.id,
            amount = bill.grandTotal, type = "REFUND",
            billId = bill.id, note = "Refund for bill #${bill.id.take(8)}",
            timestamp = System.currentTimeMillis()
        ))

        // Reverse customer balance
        // If they had paid, give back the paid amount as a credit reduction
        db.updateCustomer(customer.copy(
            totalPurchase = (customer.totalPurchase - bill.grandTotal).coerceAtLeast(0.0),
            totalPaid     = (customer.totalPaid - bill.paidAmount).coerceAtLeast(0.0),
            balance       = (customer.balance - bill.balance).coerceAtLeast(0.0)
        ))

        onSuccess()
    }

    fun getBills(onResult: (List<Bill>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllBills()) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun getBillsByDateRange(from: Long, to: Long, onResult: (List<Bill>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getBillsByDateRange(from, to)) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun recordPayment(customer: Customer, amount: Double, note: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!db.insertLedgerEntry(Ledger(
                id = UUID.randomUUID().toString(), customerId = customer.id,
                amount = amount, type = "PAYMENT", billId = "",
                note = note.ifBlank { "Standalone payment" },
                timestamp = System.currentTimeMillis()
            ))) { onError("Failed to record payment"); return }

        db.updateCustomer(customer.copy(
            totalPaid = customer.totalPaid + amount,
            balance   = customer.balance - amount
        ))
        onSuccess()
    }

    fun getLedger(customerId: String, onResult: (List<Ledger>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getLedgerByCustomer(customerId)) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }

    fun addExpense(expense: Expense, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val new = expense.copy(id = UUID.randomUUID().toString())
        if (db.insertExpense(new)) onSuccess() else onError("Failed to save expense")
    }

    fun getExpenses(onResult: (List<Expense>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllExpenses()) }
        catch (e: Exception) { onError(e.message ?: "Error") }
    }
}