package com.animan.wholesalemanager.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import java.io.File

object WhatsAppShare {

    /**
     * Shares the bill PDF directly to WhatsApp.
     * If WhatsApp is not installed, falls back to a generic share sheet.
     */
    fun shareBillViaWhatsApp(
        context: Context,
        file: File,
        customer: Customer,
        grandTotal: Double,
        balance: Double
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )

        val shopName = AppPreferences.getShopName(context)
        val message  = buildString {
            append("*$shopName — Bill Summary*\n\n")
            append("Customer: ${customer.name}\n")
            if (customer.phone.isNotBlank()) append("Phone: ${customer.phone}\n")
            append("Total: ₹${grandTotal.toInt()}\n")
            if (balance > 0) append("Balance due: ₹${balance.toInt()}\n")
            append("\nPlease find the detailed bill attached.")
        }

        // Try WhatsApp first
        val whatsAppIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(whatsAppIntent)
        } catch (e: Exception) {
            // WhatsApp not installed — fall back to generic share
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Share bill via"))
        }
    }

    /**
     * Shares just a text summary (no PDF) — useful when PDF generation is skipped.
     */
    fun shareTextSummary(
        context: Context,
        customer: Customer,
        items: List<BillItem>,
        grandTotal: Double,
        paid: Double,
        balance: Double
    ) {
        val shopName = AppPreferences.getShopName(context)
        val message = buildString {
            append("*$shopName — Invoice*\n\n")
            append("Customer: ${customer.name}\n\n")
            append("*Items:*\n")
            items.forEach { item ->
                append("• ${item.name} × ${item.quantity} ${item.unit} = ₹${(item.price * item.quantity).toInt()}\n")
            }
            append("\nTotal: ₹${grandTotal.toInt()}")
            append("\nPaid: ₹${paid.toInt()}")
            if (balance > 0) append("\nBalance due: ₹${balance.toInt()}")
            append("\n\n_Sent from $shopName billing app_")
        }

        val whatsAppIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            context.startActivity(whatsAppIntent)
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, "Share via"))
        }
    }
}