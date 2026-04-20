package com.animan.wholesalemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.utils.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    var isBackingUp  = mutableStateOf(false)
    var backupStatus = mutableStateOf("")

    private val db by lazy { DatabaseHelper(app.applicationContext) }
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    // ── Backup now ────────────────────────────────────────────────────
    fun backupNow(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { backupStatus.value = "Not logged in. Please login first."; return }

        isBackingUp.value = true
        backupStatus.value = "Backing up…"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val customers = db.getAllCustomers()
                val products  = db.getAllProducts()
                val bills     = db.getAllBills()
                val expenses  = db.getAllExpenses()

                val userDoc = firestore.collection("backups").document(uid)

                // Write each collection in batches (Firestore limit: 500 ops per batch)
                userDoc.collection("customers").writeAll(
                    customers.map { mapOf("id" to it.id, "name" to it.name,
                        "phone" to it.phone, "address" to it.address,
                        "totalPurchase" to it.totalPurchase, "totalPaid" to it.totalPaid,
                        "balance" to it.balance) })

                userDoc.collection("products").writeAll(
                    products.map { mapOf("id" to it.id, "name" to it.name,
                        "sellingPrice" to it.sellingPrice, "costPrice" to it.costPrice,
                        "quantity" to it.quantity, "unit" to it.unit,
                        "category" to it.category, "minStockLevel" to it.minStockLevel,
                        "barcode" to it.barcode, "gstPercent" to it.gstPercent) })

                userDoc.collection("expenses").writeAll(
                    expenses.map { mapOf("id" to it.id, "title" to it.title,
                        "amount" to it.amount, "date" to it.date) })

                // Bills with nested items
                bills.forEach { bill ->
                    userDoc.collection("bills").document(bill.id).set(mapOf(
                        "id" to bill.id, "customerId" to bill.customerId,
                        "customerName" to bill.customerName,
                        "itemsTotal" to bill.itemsTotal, "gstTotal" to bill.gstTotal,
                        "grandTotal" to bill.grandTotal, "paidAmount" to bill.paidAmount,
                        "balance" to bill.balance, "timestamp" to bill.timestamp,
                        "isRefunded" to bill.isRefunded,
                        "items" to bill.items.map { i -> mapOf(
                            "productId" to i.productId, "name" to i.name,
                            "price" to i.price, "costPrice" to i.costPrice,
                            "unit" to i.unit, "quantity" to i.quantity,
                            "gstPercent" to i.gstPercent) }
                    )).await()
                }

                // Write metadata
                val nowStr = dateFormat.format(Date())
                userDoc.set(mapOf(
                    "lastBackup" to nowStr,
                    "customerCount" to customers.size,
                    "productCount" to products.size,
                    "billCount" to bills.size,
                    "expenseCount" to expenses.size
                )).await()

                AppPreferences.setLastBackupTime(context, nowStr)

                withContext(Dispatchers.Main) {
                    isBackingUp.value = false
                    backupStatus.value = "Backup complete — $nowStr"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBackingUp.value = false
                    backupStatus.value = "Error: ${e.message}"
                }
            }
        }
    }

    // ── Restore ───────────────────────────────────────────────────────
    fun restoreNow(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { backupStatus.value = "Not logged in."; return }

        isBackingUp.value = true
        backupStatus.value = "Restoring…"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userDoc = firestore.collection("backups").document(uid)

                val customerDocs = userDoc.collection("customers").get().await()
                val productDocs  = userDoc.collection("products").get().await()
                val expenseDocs  = userDoc.collection("expenses").get().await()
                val billDocs     = userDoc.collection("bills").get().await()

                // Import customers
                customerDocs.documents.forEach { doc ->
                    val c = com.animan.wholesalemanager.data.local.Customer(
                        id = doc.getString("id") ?: return@forEach,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        address = doc.getString("address") ?: "",
                        totalPurchase = doc.getDouble("totalPurchase") ?: 0.0,
                        totalPaid = doc.getDouble("totalPaid") ?: 0.0,
                        balance = doc.getDouble("balance") ?: 0.0
                    )
                    // Insert or update — ignore if already exists
                    runCatching { db.insertCustomer(c) }
                    runCatching { db.updateCustomer(c) }
                }

                // Import products
                productDocs.documents.forEach { doc ->
                    val p = com.animan.wholesalemanager.data.local.Product(
                        id = doc.getString("id") ?: return@forEach,
                        name = doc.getString("name") ?: "",
                        sellingPrice = doc.getDouble("sellingPrice") ?: 0.0,
                        costPrice = doc.getDouble("costPrice") ?: 0.0,
                        quantity = (doc.getLong("quantity") ?: 0L).toInt(),
                        unit = doc.getString("unit") ?: "Piece",
                        category = doc.getString("category") ?: "",
                        minStockLevel = (doc.getLong("minStockLevel") ?: 5L).toInt(),
                        barcode = doc.getString("barcode") ?: "",
                        gstPercent = doc.getDouble("gstPercent") ?: 0.0
                    )
                    runCatching { db.insertProduct(p) }
                    runCatching { db.updateProduct(p) }
                }

                // Import expenses
                expenseDocs.documents.forEach { doc ->
                    val e = com.animan.wholesalemanager.data.local.Expense(
                        id = doc.getString("id") ?: return@forEach,
                        title = doc.getString("title") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getLong("date") ?: 0L
                    )
                    runCatching { db.insertExpense(e) }
                }

                withContext(Dispatchers.Main) {
                    isBackingUp.value = false
                    backupStatus.value = "Restore complete."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBackingUp.value = false
                    backupStatus.value = "Restore error: ${e.message}"
                }
            }
        }
    }

    // Helper: write a list of maps to a Firestore sub-collection
    private suspend fun com.google.firebase.firestore.CollectionReference.writeAll(
        items: List<Map<String, Any?>>
    ) {
        items.forEach { item ->
            val id = item["id"] as? String ?: return@forEach
            document(id).set(item).await()
        }
    }
}