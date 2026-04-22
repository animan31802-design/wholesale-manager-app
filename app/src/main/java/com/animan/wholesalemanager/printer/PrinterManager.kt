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
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager {

    private val printer = BluetoothPrinter()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

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

        if (adapter == null || !adapter.isEnabled) return null to "Bluetooth is not enabled"

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
        finalAmount: Double    // total owed = itemsTotal + previousBalance
    ): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"

        val shopName     = AppPreferences.getShopName(context)
        val balance      = finalAmount - paidAmount
        val gstTotal     = items.sumOf { it.gstAmount }
        val billNo       = "B${System.currentTimeMillis().toString().takeLast(6)}"
        val now          = dateFormat.format(Date())

        printer.print(buildReceipt(
            shopName     = shopName,
            billNo       = billNo,
            dateTime     = now,
            customer     = customer,
            items        = items,
            itemsTotal   = itemsTotal,
            gstTotal     = gstTotal,
            grandTotal   = itemsTotal + gstTotal,
            previousBal  = finalAmount - (itemsTotal + gstTotal),
            totalPayable = finalAmount,
            paidAmount   = paidAmount,
            balance      = balance
        ))
        printer.disconnect()
        return "Printed successfully"
    }

    fun printTestBill(context: Context): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"
        val shopName = AppPreferences.getShopName(context)
        val now = dateFormat.format(Date())
        printer.print(buildTestReceipt(shopName, now))
        printer.disconnect()
        return "Test print successful"
    }

    private fun buildReceipt(
        shopName: String,
        billNo: String,
        dateTime: String,
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        gstTotal: Double,
        grandTotal: Double,
        previousBal: Double,
        totalPayable: Double,
        paidAmount: Double,
        balance: Double
    ): String {
        val W = 32   // receipt width in chars
        val sb = StringBuilder()

        // ── Header ────────────────────────────────────────────────────
        sb.append("\n")
        sb.append(center(shopName.uppercase(), W))
        sb.append(center("TAX INVOICE", W))
        sb.append(divider(W))

        // Bill info
        sb.append(twoCol("Bill No: $billNo", dateTime, W))
        sb.append(divider(W))

        // Customer info
        sb.append("To: ${customer.name}\n")
        if (customer.phone.isNotBlank()) sb.append("Ph: ${customer.phone}\n")
        if (customer.address.isNotBlank()) sb.append("Addr: ${customer.address.take(28)}\n")
        sb.append(divider(W))

        // ── Items header ──────────────────────────────────────────────
        sb.append(itemHeader(W))
        sb.append(thinDivider(W))

        // ── Items ─────────────────────────────────────────────────────
        items.forEach { item ->
            val lineTotal = item.price * item.quantity
            // Line 1: name
            sb.append("${item.name.take(20)}\n")
            // Line 2: qty × price = total  [GST if applicable]
            val gstStr = if (item.gstPercent > 0) " GST${item.gstPercent.toInt()}%" else ""
            sb.append(
                String.format(
                    "  %s %s x %s = %s\n",
                    item.unit.take(3),
                    formatAmt(item.quantity.toDouble()),
                    formatAmt(item.price),
                    formatAmt(lineTotal)
                ) + if (item.gstPercent > 0)
                    "  GST ${item.gstPercent.toInt()}%: Rs.${formatAmt(item.gstAmount)}\n"
                else ""
            )
        }

        sb.append(thinDivider(W))

        // ── Totals ────────────────────────────────────────────────────
        sb.append(amtRow("Items total", "Rs.${formatAmt(itemsTotal)}", W))

        if (gstTotal > 0) {
            sb.append(amtRow("GST total", "Rs.${formatAmt(gstTotal)}", W))
        }

        sb.append(amtRow("Grand total", "Rs.${formatAmt(grandTotal)}", W))

        if (previousBal > 0.01) {
            sb.append(amtRow("Previous due", "Rs.${formatAmt(previousBal)}", W))
            sb.append(amtRow("Total payable", "Rs.${formatAmt(totalPayable)}", W))
        }

        sb.append(divider(W))
        sb.append(amtRow("Paid", "Rs.${formatAmt(paidAmount)}", W))

        if (balance > 0.01) {
            sb.append(amtRow("Balance due", "Rs.${formatAmt(balance)}", W))
        } else {
            sb.append(center("** PAID IN FULL **", W))
        }

        sb.append(divider(W))
        sb.append(center("Thank You! Visit Again", W))
        sb.append(center("Keep this receipt safe", W))
        sb.append("\n\n\n")

        return sb.toString()
    }

    private fun buildTestReceipt(shopName: String, now: String): String {
        val W = 32
        val sb = StringBuilder()
        sb.append("\n")
        sb.append(center(shopName.uppercase(), W))
        sb.append(center("TAX INVOICE", W))
        sb.append(divider(W))
        sb.append(twoCol("Bill No: T001", now, W))
        sb.append(divider(W))
        sb.append("To: TEST CUSTOMER\n")
        sb.append("Ph: 9999999999\n")
        sb.append(divider(W))
        sb.append(itemHeader(W))
        sb.append(thinDivider(W))
        sb.append("Rice\n")
        sb.append("  Kg  2 x 50.00 = 100.00\n")
        sb.append("Oil\n")
        sb.append("  L   1 x 100.00 = 100.00\n")
        sb.append("  GST 5%: Rs.5.00\n")
        sb.append(thinDivider(W))
        sb.append(amtRow("Items total", "Rs.200.00", W))
        sb.append(amtRow("GST total", "Rs.5.00", W))
        sb.append(amtRow("Grand total", "Rs.205.00", W))
        sb.append(divider(W))
        sb.append(amtRow("Paid", "Rs.205.00", W))
        sb.append(center("** PAID IN FULL **", W))
        sb.append(divider(W))
        sb.append(center("Thank You! Visit Again", W))
        sb.append("\n\n\n")
        return sb.toString()
    }

    // ── Layout helpers ────────────────────────────────────────────────

    private fun divider(w: Int) = "-".repeat(w) + "\n"
    private fun thinDivider(w: Int) = "- ".repeat(w / 2) + "\n"

    private fun center(text: String, w: Int): String {
        if (text.length >= w) return text.take(w) + "\n"
        val pad = (w - text.length) / 2
        return " ".repeat(pad) + text + "\n"
    }

    private fun twoCol(left: String, right: String, w: Int): String {
        val gap = w - left.length - right.length
        return if (gap > 0) left + " ".repeat(gap) + right + "\n"
        else left.take(w - right.length - 1) + " " + right + "\n"
    }

    private fun amtRow(label: String, value: String, w: Int): String {
        val gap = w - label.length - value.length
        return if (gap > 0) label + " ".repeat(gap) + value + "\n"
        else label.take(w - value.length - 1) + " " + value + "\n"
    }

    private fun itemHeader(w: Int) =
        String.format("%-20s %${w - 20}s\n", "Item", "Amount")

    private fun formatAmt(v: Double): String = "%.2f".format(v)
}