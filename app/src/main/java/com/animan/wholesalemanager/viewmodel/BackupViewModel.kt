package com.animan.wholesalemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.data.local.Bill
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.data.local.Expense
import com.animan.wholesalemanager.data.local.Ledger
import com.animan.wholesalemanager.data.local.Product
import com.animan.wholesalemanager.data.local.Purchase
import com.animan.wholesalemanager.data.local.PurchaseItem
import com.animan.wholesalemanager.data.local.Supplier
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.work.BackupWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    var isBackingUp    = mutableStateOf(false)
    var backupStatus   = mutableStateOf("")
    var lastBackupTime = mutableStateOf(
        AppPreferences.getLastBackupTime(app.applicationContext)
    )

    // ── Public ViewModel functions (for UI) ───────────────────────────

    fun backupNow(context: Context) {
        if (isBackingUp.value) return
        isBackingUp.value  = true
        backupStatus.value = "Backing up…"

        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)

        // Poll until BackupWorker updates the timestamp (max 30 s)
        viewModelScope.launch(Dispatchers.IO) {
            var waited = 0
            while (waited < 30_000) {
                delay(1_000)
                waited += 1_000
                val newTime = AppPreferences.getLastBackupTime(context)
                if (newTime != lastBackupTime.value) {
                    withContext(Dispatchers.Main) {
                        lastBackupTime.value = newTime
                        backupStatus.value   = "Backup complete — $newTime"
                        isBackingUp.value    = false
                    }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) {
                backupStatus.value = "Check your internet connection and try again."
                isBackingUp.value  = false
            }
        }
    }

    fun restoreNow(context: Context) {
        if (isBackingUp.value) return
        isBackingUp.value  = true
        backupStatus.value = "Restoring…"
        viewModelScope.launch {
            val result = try {
                performRestore(context)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            backupStatus.value = result
            isBackingUp.value  = false
        }
    }

    // ── Suspend helpers (called by AuthViewModel) ─────────────────────

    /**
     * Runs the BackupWorker synchronously via a OneTimeWorkRequest and waits
     * for it to complete (max 60 s). Throws if it fails or times out.
     */
    suspend fun backupNowSuspend(context: Context) {
        withContext(Dispatchers.IO) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            val wm = WorkManager.getInstance(context)
            wm.enqueue(request)

            // Wait for completion (max 60 s)
            val before = AppPreferences.getLastBackupTime(context)
            var waited = 0
            while (waited < 60_000) {
                delay(1_000)
                waited += 1_000
                val info = wm.getWorkInfoById(request.id).await()
                if (info?.state == WorkInfo.State.SUCCEEDED) return@withContext
                if (info?.state == WorkInfo.State.FAILED)
                    throw Exception("Backup worker failed")
            }
            throw Exception("Backup timed out")
        }
    }

    /**
     * Restores from Firebase into local SQLite. Returns a summary string.
     * Safe to call even when there is no backup (returns "No backup found").
     */
    suspend fun restoreNowSuspend(context: Context): String =
        withContext(Dispatchers.IO) { performRestore(context) }

    // ── Core restore logic ────────────────────────────────────────────

    private suspend fun performRestore(context: Context): String =
        withContext(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@withContext "Not logged in"

            val firestore = FirebaseFirestore.getInstance()
            val userDoc   = firestore.collection("backups").document(uid)

            // Check backup exists
            val meta = userDoc.get().await()
            if (!meta.exists()) return@withContext "No backup found"

            val db = DatabaseHelper(context)

            // ── Customers ─────────────────────────────────────────────
            val customerDocs = userDoc.collection("customers").get().await()
            val customers = customerDocs.documents.mapNotNull { d ->
                try {
                    Customer(
                        id            = d.getString("id") ?: return@mapNotNull null,
                        name          = d.getString("name") ?: "",
                        phone         = d.getString("phone") ?: "",
                        address       = d.getString("address") ?: "",
                        latitude      = d.getDouble("latitude"),
                        longitude     = d.getDouble("longitude"),
                        totalPurchase = d.getDouble("totalPurchase") ?: 0.0,
                        totalPaid     = d.getDouble("totalPaid") ?: 0.0,
                        balance       = d.getDouble("balance") ?: 0.0
                    )
                } catch (_: Exception) { null }
            }

            // ── Products ──────────────────────────────────────────────
            val productDocs = userDoc.collection("products").get().await()
            val products = productDocs.documents.mapNotNull { d ->
                try {
                    Product(
                        id            = d.getString("id") ?: return@mapNotNull null,
                        name          = d.getString("name") ?: "",
                        sellingPrice  = d.getDouble("sellingPrice") ?: 0.0,
                        costPrice     = d.getDouble("costPrice") ?: 0.0,
                        quantity      = d.getDouble("quantity") ?: 0.0,
                        unit          = d.getString("unit") ?: "Piece",
                        category      = d.getString("category") ?: "",
                        minStockLevel = d.getDouble("minStockLevel") ?: 5.0,
                        barcode       = d.getString("barcode") ?: "",
                        gstPercent    = d.getDouble("gstPercent") ?: 0.0,
                        allowPartial = when (val v = d.get("allowPartial")) {
                            is Boolean -> v
                            is Number  -> v.toInt() == 1
                            is String  -> v.equals("true", true) || v == "1"
                            else -> false
                        }
                    )
                } catch (_: Exception) { null }
            }

            // ── Expenses ──────────────────────────────────────────────
            val expenseDocs = userDoc.collection("expenses").get().await()
            val expenses = expenseDocs.documents.mapNotNull { d ->
                try {
                    Expense(
                        id     = d.getString("id") ?: return@mapNotNull null,
                        title  = d.getString("title") ?: "",
                        amount = d.getDouble("amount") ?: 0.0,
                        date   = d.getLong("date") ?: 0L
                    )
                } catch (_: Exception) { null }
            }

            val supplierDocs = userDoc.collection("suppliers").get().await()
            val suppliers = supplierDocs.documents.mapNotNull { d ->
                try {
                    Supplier(
                        id = d.getString("id") ?: return@mapNotNull null,
                        name = d.getString("name") ?: "",
                        phone = d.getString("phone") ?: "",
                        address = d.getString("address") ?: "",
                        balance = d.getDouble("balance") ?: 0.0
                    )
                } catch (_: Exception) { null }
            }

            val purchaseDocs = userDoc.collection("purchases").get().await()
            val purchases = purchaseDocs.documents.mapNotNull { d ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = d.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawItems.map { i ->
                        PurchaseItem(
                            productId = i["productId"] as? String ?: "",
                            name = i["name"] as? String ?: "",
                            costPrice = (i["costPrice"] as? Number)?.toDouble() ?: 0.0,
                            previousCostPrice = (i["previousCostPrice"] as? Number)?.toDouble()
                                ?: 0.0,
                            unit = i["unit"] as? String ?: "Piece",
                            quantity = (i["quantity"] as? Number)?.toDouble() ?: 1.0,
                            gstPercent = (i["gstPercent"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                    Purchase(
                        id = d.getString("id") ?: return@mapNotNull null,
                        supplierId = d.getString("supplierId") ?: "",
                        supplierName = d.getString("supplierName") ?: "",
                        poNumber = d.getString("poNumber") ?: "",
                        itemsTotal = d.getDouble("itemsTotal") ?: 0.0,
                        gstTotal = d.getDouble("gstTotal") ?: 0.0,
                        grandTotal = d.getDouble("grandTotal") ?: 0.0,
                        paidAmount = d.getDouble("paidAmount") ?: 0.0,
                        balance = d.getDouble("balance") ?: 0.0,
                        timestamp = d.getLong("timestamp") ?: 0L,
                        note = d.getString("note") ?: "",
                        items = items
                    )
                } catch (_: Exception) { null }
            }

            // ── Bills (with embedded items) ───────────────────────────
            val billDocs = userDoc.collection("bills").get().await()
            val bills = billDocs.documents.mapNotNull { d ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = d.get("items") as? List<Map<String, Any>> ?: emptyList()
                    val items = rawItems.map { i ->
                        BillItem(
                            productId  = i["productId"] as? String ?: "",
                            name       = i["name"] as? String ?: "",
                            price      = (i["price"] as? Number)?.toDouble() ?: 0.0,
                            costPrice  = (i["costPrice"] as? Number)?.toDouble() ?: 0.0,
                            unit       = i["unit"] as? String ?: "Piece",
                            quantity   = (i["quantity"] as? Number)?.toDouble() ?: 1.0,
                            gstPercent = (i["gstPercent"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                    Bill(
                        id           = d.getString("id") ?: return@mapNotNull null,
                        customerId   = d.getString("customerId") ?: "",
                        customerName = d.getString("customerName") ?: "",
                        itemsTotal   = d.getDouble("itemsTotal") ?: 0.0,
                        gstTotal     = d.getDouble("gstTotal") ?: 0.0,
                        grandTotal   = d.getDouble("grandTotal") ?: 0.0,
                        paidAmount   = d.getDouble("paidAmount") ?: 0.0,
                        balance      = d.getDouble("balance") ?: 0.0,
                        timestamp    = d.getLong("timestamp") ?: 0L,
                        isRefunded   = d.getBoolean("isRefunded") ?: false,
                        refundedAt   = d.getLong("refundedAt") ?: 0L,
                        items        = items
                    )
                } catch (_: Exception) { null }
            }

            // ── Ledger entries ────────────────────────────────────────
            val ledgerDocs = userDoc.collection("ledger").get().await()
            val ledgerEntries = ledgerDocs.documents.mapNotNull { d ->
                try {
                    Ledger(
                        id         = d.getString("id") ?: return@mapNotNull null,
                        customerId = d.getString("customerId") ?: "",
                        amount     = d.getDouble("amount") ?: 0.0,
                        type       = d.getString("type") ?: "",
                        billId     = d.getString("billId") ?: "",
                        note       = d.getString("note") ?: "",
                        timestamp  = d.getLong("timestamp") ?: 0L
                    )
                } catch (_: Exception) { null }
            }

            // ── Write to SQLite (clear first to avoid PK conflicts) ───
            // Order matters: customers & products before bills (FK), bills before ledger
            db.clearAllData()

            customers.forEach    { db.insertCustomer(it) }
            products.forEach     { db.insertProduct(it) }
            bills.forEach        { db.insertBillWithItems(it) }
            ledgerEntries.forEach{ db.insertLedgerEntry(it) }
            expenses.forEach     { db.insertExpense(it) }
            suppliers.forEach    { db.insertSupplier(it) }
            purchases.forEach    { db.insertPurchaseWithItems(it) }

            "Restored ${customers.size} customers, ${products.size} products, " +
                    "${bills.size} bills, ${expenses.size} expenses" +
            "${suppliers.size} suppliers, ${purchases.size} purchases"
        }
}