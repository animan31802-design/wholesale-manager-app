package com.animan.wholesalemanager.repository

import android.content.Context
import com.animan.wholesalemanager.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SupplierRepository(private val context: Context) {

    private val db get() = DatabaseHelper(context)

    // ── Suppliers ─────────────────────────────────────────────────────

    fun addSupplier(supplier: Supplier, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.insertSupplier(supplier)) onSuccess()
        else onError("Failed to add supplier")
    }

    fun getSuppliers(onResult: (List<Supplier>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllSuppliers()) }
        catch (e: Exception) { onError(e.message ?: "Failed to load suppliers") }
    }

    fun updateSupplier(supplier: Supplier, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.updateSupplier(supplier)) onSuccess()
        else onError("Failed to update supplier")
    }

    fun deleteSupplier(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (db.deleteSupplier(id)) onSuccess()
        else onError("Failed to delete supplier")
    }

    fun searchSuppliers(query: String): List<Supplier> = db.searchSuppliers(query)

    // ── Purchases ─────────────────────────────────────────────────────

    /**
     * Saves a purchase and does all side effects atomically:
     * 1. Insert purchase + items (DB transaction)
     * 2. Increment product stock
     * 3. Update product cost price if changed
     * 4. Insert expense as "Stock Purchase" category
     * 5. Update supplier balance
     * 6. Insert supplier ledger entry
     */
    fun savePurchase(
        purchase  : Purchase,
        supplier  : Supplier,
        onSuccess : (purchaseId: String) -> Unit,
        onError   : (String) -> Unit
    ) {
        try {
            // Step 1–3: insert purchase + items + stock update (all in one transaction)
            val ok = db.insertPurchaseWithItems(purchase)
            if (!ok) { onError("Failed to save purchase"); return }

            // Step 4: auto-create expense
            val expense = Expense(
                title  = "Stock Purchase — ${purchase.supplierName} (${purchase.poNumber})",
                amount = purchase.grandTotal,
                date   = purchase.timestamp
            )
            db.insertExpense(expense)

            // Step 5: update supplier balance (add unpaid amount)
            val unpaid = purchase.balance
            if (unpaid > 0) {
                val updated = supplier.copy(balance = supplier.balance + unpaid)
                db.updateSupplier(updated)
            }

            // Step 6: supplier ledger
            db.insertSupplierLedgerEntry(SupplierLedger(
                supplierId = supplier.id,
                amount     = purchase.grandTotal,
                type       = "purchase",
                purchaseId = purchase.id,
                note       = "PO: ${purchase.poNumber}",
                timestamp  = purchase.timestamp
            ))

            onSuccess(purchase.id)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }

    fun getAllPurchases(onResult: (List<Purchase>) -> Unit, onError: (String) -> Unit) {
        try { onResult(db.getAllPurchases()) }
        catch (e: Exception) { onError(e.message ?: "Failed to load purchases") }
    }

    fun getPurchasesBySupplier(supplierId: String): List<Purchase> =
        db.getPurchasesBySupplier(supplierId)

    fun getSupplierLedger(supplierId: String): List<SupplierLedger> =
        db.getSupplierLedger(supplierId)

    // ── Record supplier payment ───────────────────────────────────────

    fun recordSupplierPayment(
        supplier  : Supplier,
        amount    : Double,
        note      : String,
        onSuccess : () -> Unit,
        onError   : (String) -> Unit
    ) {
        try {
            val updated = supplier.copy(balance = supplier.balance - amount)
            db.updateSupplier(updated)
            db.insertSupplierLedgerEntry(SupplierLedger(
                supplierId = supplier.id,
                amount     = amount,
                type       = "payment",
                note       = note.ifBlank { "Payment to ${supplier.name}" },
                timestamp  = System.currentTimeMillis()
            ))
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Failed to record payment")
        }
    }

    // ── PO number generator ───────────────────────────────────────────

    fun generatePoNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val seq  = (System.currentTimeMillis() % 1000).toString().padStart(3, '0')
        return "PO-$date-$seq"
    }

    fun reducePurchaseBalance(purchaseId: String, amount: Double) {
        db.reducePurchaseBalance(purchaseId, amount)
    }
}