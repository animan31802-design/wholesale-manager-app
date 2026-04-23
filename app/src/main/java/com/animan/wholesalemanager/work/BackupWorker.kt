package com.animan.wholesalemanager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.utils.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class BackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure()   // not logged in — skip silently

        return try {
            val db        = DatabaseHelper(context)
            val firestore = FirebaseFirestore.getInstance()
            val userDoc   = firestore.collection("backups").document(uid)
            val dateStr   = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date())

            val customers = db.getAllCustomers()
            val products  = db.getAllProducts()
            val expenses  = db.getAllExpenses()
            val bills     = db.getAllBills()

            // Write customers
            customers.forEach { c ->
                userDoc.collection("customers").document(c.id).set(mapOf(
                    "id" to c.id, "name" to c.name, "phone" to c.phone,
                    "address" to c.address, "latitude" to c.latitude,
                    "longitude" to c.longitude,
                    "totalPurchase" to c.totalPurchase, "totalPaid" to c.totalPaid,
                    "balance" to c.balance
                )).await()
            }

            // Write products
            products.forEach { p ->
                userDoc.collection("products").document(p.id).set(mapOf(
                    "id" to p.id, "name" to p.name,
                    "sellingPrice" to p.sellingPrice, "costPrice" to p.costPrice,
                    "quantity" to p.quantity, "unit" to p.unit,
                    "category" to p.category, "minStockLevel" to p.minStockLevel,
                    "barcode" to p.barcode, "gstPercent" to p.gstPercent
                )).await()
            }

            // Write expenses
            expenses.forEach { e ->
                userDoc.collection("expenses").document(e.id).set(mapOf(
                    "id" to e.id, "title" to e.title,
                    "amount" to e.amount, "date" to e.date
                )).await()
            }

            // Write bills with items
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
                        "gstPercent" to i.gstPercent
                    )}
                )).await()
            }

            // Write metadata + timestamp
            userDoc.set(mapOf(
                "lastBackup"     to dateStr,
                "customerCount"  to customers.size,
                "productCount"   to products.size,
                "billCount"      to bills.size,
                "expenseCount"   to expenses.size
            )).await()

            // Save time locally — this is what the Settings screen reads
            AppPreferences.setLastBackupTime(context, dateStr)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()  // WorkManager will retry on failure
        }
    }
}