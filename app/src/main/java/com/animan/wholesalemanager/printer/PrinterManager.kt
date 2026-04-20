package com.animan.wholesalemanager.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.BluetoothPrinter
import com.animan.wholesalemanager.utils.PrinterPreferences

class PrinterManager {

    private val printer = BluetoothPrinter()

    private fun resolveDevice(context: Context): Pair<BluetoothDevice?, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) return null to "Bluetooth permission not granted"

        val adapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } else {
            @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
        }

        if (adapter == null || !adapter.isEnabled)
            return null to "Bluetooth is not enabled"

        val devices = try { adapter.bondedDevices } catch (e: SecurityException) {
            return null to "Bluetooth permission denied"
        }

        val savedAddress = PrinterPreferences.getSavedAddress(context)
        if (savedAddress != null) {
            val device = devices?.firstOrNull { it.address == savedAddress }
            if (device != null) return device to ""
            return null to "Saved printer not found. Re-select in Settings."
        }

        val fallback = devices?.firstOrNull {
            try { it.name.contains("printer", ignoreCase = true) }
            catch (_: SecurityException) { false }
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
        finalAmount: Double
    ): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"
        val shopName = AppPreferences.getShopName(context)
        val balance = finalAmount - paidAmount
        printer.print(buildReceipt(shopName, customer.name, items, itemsTotal, paidAmount, balance))
        printer.disconnect()
        return "Printed successfully"
    }

    fun printTestBill(context: Context): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"
        val shopName = AppPreferences.getShopName(context)
        printer.print(buildTestReceipt(shopName))
        printer.disconnect()
        return "Test print successful"
    }

    private fun buildReceipt(
        shopName: String,
        customerName: String,
        items: List<BillItem>,
        total: Double,
        paid: Double,
        balance: Double
    ): String {
        val sb = StringBuilder()
        sb.append("\n")
        sb.append(centerText(shopName))
        sb.append("------------------------------\n")
        sb.append("Customer: $customerName\n")
        sb.append("------------------------------\n")
        sb.append(formatRow("Item", "Qty", "Price", "Total"))
        sb.append("------------------------------\n")
        items.forEach {
            sb.append(formatRow(
                it.name.take(10),
                "${it.quantity}${it.unit.take(2)}",
                it.price.toInt().toString(),
                (it.price * it.quantity).toInt().toString()
            ))
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

    private fun buildTestReceipt(shopName: String) = """
        ${shopName.take(30)}
        ------------------------------
        Customer: TEST USER
        ------------------------------
        Item        Qty   Price   Total
        Rice         2Kg     50     100
        Oil          1L     100     100
        ------------------------------
        Total:              200
        Paid:               150
        Bal:                 50
        ------------------------------
        Thank You! Visit Again
        
        
        
    """.trimIndent()

    private fun centerText(text: String): String {
        val w = 30; val p = (w - text.length) / 2
        return " ".repeat(p.coerceAtLeast(0)) + text + "\n"
    }

    private fun rightAlign(text: String): String {
        val w = 30
        return " ".repeat((w - text.length).coerceAtLeast(0)) + text + "\n"
    }

    private fun formatRow(c1: String, c2: String, c3: String, c4: String): String =
        String.format("%-10s %4s %5s %6s\n", c1, c2, c3, c4)
}