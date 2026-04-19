package com.animan.wholesalemanager.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.utils.BluetoothPrinter
import com.animan.wholesalemanager.utils.PrinterPreferences

class PrinterManager {

    private val printer = BluetoothPrinter()

    private fun getPairedDevices(context: Context): Set<BluetoothDevice>? {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return null
        return BluetoothAdapter.getDefaultAdapter()?.bondedDevices
    }

    // Resolves the saved printer address from SharedPreferences.
    // Falls back to any device whose name contains "printer" if nothing saved.
    private fun resolveDevice(context: Context): Pair<BluetoothDevice?, String> {
        val devices = getPairedDevices(context)
            ?: return null to "Bluetooth permission not granted"

        if (devices.isEmpty()) return null to "No paired Bluetooth devices found"

        val savedAddress = PrinterPreferences.getSavedAddress(context)

        if (savedAddress != null) {
            val device = devices.firstOrNull { it.address == savedAddress }
            if (device != null) return device to ""
            return null to "Saved printer not found — please re-select in Settings"
        }

        // Fallback: first device whose name contains "printer" (case-insensitive)
        val fallback = devices.firstOrNull {
            try { it.name.contains("printer", ignoreCase = true) }
            catch (e: SecurityException) { false }
        }
        return if (fallback != null) fallback to ""
        else null to "No printer selected. Go to Settings → Select printer."
    }

    fun printBill(
        context: Context,
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        paidAmount: Double,
        finalAmount: Double    // total owed (itemsTotal + previous balance)
    ): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error

        if (!printer.connect(device)) return "Printer connection failed"

        val balance = finalAmount - paidAmount
        val receipt = buildReceipt(customer.name, items, itemsTotal, paidAmount, balance)
        printer.print(receipt)
        printer.disconnect()
        return "Printed successfully"
    }

    fun printTestBill(context: Context): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error

        if (!printer.connect(device)) return "Printer connection failed"

        val receipt = buildTestReceipt()
        printer.print(receipt)
        printer.disconnect()
        return "Test print successful"
    }

    private fun buildReceipt(
        customerName: String,
        items: List<BillItem>,
        total: Double,
        paid: Double,
        balance: Double
    ): String {
        val sb = StringBuilder()
        sb.append("\n")
        sb.append(centerText("MY SHOP"))
        sb.append("------------------------------\n")
        sb.append("Customer: $customerName\n")
        sb.append("------------------------------\n")
        sb.append(formatRow("Item", "Qty", "Price", "Total"))
        sb.append("------------------------------\n")

        items.forEach {
            val itemTotal = it.price * it.quantity
            sb.append(
                formatRow(
                    it.name.take(10),
                    it.quantity.toString(),
                    it.price.toInt().toString(),
                    itemTotal.toInt().toString()
                )
            )
        }

        sb.append("------------------------------\n")
        sb.append(rightAlign("Total: Rs.${total.toInt()}"))
        sb.append(rightAlign("Paid:  Rs.${paid.toInt()}"))
        sb.append(rightAlign("Bal:   Rs.${balance.toInt()}"))
        sb.append("------------------------------\n")
        sb.append(centerText("Thank You! Visit Again"))
        sb.append("\n\n\n")
        return sb.toString()
    }

    private fun buildTestReceipt(): String {
        return """
            |MY SHOP
            |------------------------------
            |Customer: TEST USER
            |------------------------------
            |Item        Qty   Price   Total
            |Rice          2      50     100
            |Oil           1     100     100
            |------------------------------
            |Total:              200
            |Paid:               150
            |Bal:                 50
            |------------------------------
            |Thank You! Visit Again
            |
            |
            |
        """.trimMargin()
    }

    private fun centerText(text: String): String {
        val width = 30
        val padding = (width - text.length) / 2
        return " ".repeat(padding.coerceAtLeast(0)) + text + "\n"
    }

    private fun rightAlign(text: String): String {
        val width = 30
        return " ".repeat((width - text.length).coerceAtLeast(0)) + text + "\n"
    }

    private fun formatRow(col1: String, col2: String, col3: String, col4: String): String {
        return String.format("%-10s %3s %6s %6s\n", col1, col2, col3, col4)
    }
}