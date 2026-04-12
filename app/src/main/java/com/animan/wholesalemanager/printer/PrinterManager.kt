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

class PrinterManager {

    private val printer = BluetoothPrinter()

    // Get paired devices safely
    private fun getPairedDevices(context: Context): Set<BluetoothDevice>? {

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.bondedDevices
    }

    // PRINT REAL BILL
    fun printBill(
        context: Context,
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        paidAmount: Double,
        finalAmount: Double
    ): String {

        val devices = getPairedDevices(context)
            ?: return "Bluetooth permission not granted"

        // Debug devices
        devices.forEach {
            try {
                println("Device: ${it.name}")
            } catch (e: SecurityException) {
                println("No permission to read device name")
            }
        }

        val device = devices.firstOrNull {
            try {
                it.name.equals("BTprinter013830", ignoreCase = true)
            } catch (e: SecurityException) {
                false
            }
        } ?: return "Printer not found (BTprinter013830)"

        val connected = printer.connect(device)
        if (!connected) return "Printer connection failed"

        val balance = finalAmount - paidAmount

        val receipt = buildReceipt(
            customer.name,
            items,
            itemsTotal,
            paidAmount,
            balance
        )

        printer.print(receipt)
        printer.disconnect()

        return "Printed successfully"
    }

    // PRINT TEST BILL
    fun printTestBill(context: Context): String {

        val devices = getPairedDevices(context)
            ?: return "Bluetooth permission not granted"

        devices.forEach {
            try {
                println("Device: ${it.name}")
            } catch (e: SecurityException) {
                println("No permission to read device name")
            }
        }

        val device = devices.firstOrNull {
            try {
                it.name.equals("BTprinter013830", ignoreCase = true)
            } catch (e: SecurityException) {
                false
            }
        } ?: return "Printer not found (BTprinter013830)"

        val connected = printer.connect(device)
        if (!connected) return "Printer connection failed"

        val receipt = """
            MY SHOP
            ------------------------------
            Customer: TEST USER
            ------------------------------
            Item        Qty   Price   Total
            Rice         2     50      100
            Oil          1     100     100
            ------------------------------
            Total:              200
            Paid:               150
            Balance:            50
            ------------------------------
            Thank You! Visit Again
            
            
            
        """.trimIndent()

        printer.print(receipt + "\n\n\n")
        printer.disconnect()

        return "Test print successful"
    }

    // Receipt Builder
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

        sb.append(rightAlign("Total: ₹${total.toInt()}"))
        sb.append(rightAlign("Paid: ₹${paid.toInt()}"))
        sb.append(rightAlign("Balance: ₹${balance.toInt()}"))

        sb.append("------------------------------\n")
        sb.append(centerText("Thank You! Visit Again\n\n\n"))

        return sb.toString()
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