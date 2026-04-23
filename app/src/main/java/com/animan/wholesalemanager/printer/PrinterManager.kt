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
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.BluetoothPrinter
import com.animan.wholesalemanager.utils.Language
import com.animan.wholesalemanager.utils.PrinterPreferences
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager {

    private val printer    = BluetoothPrinter()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    // ── 3-inch thermal printer = 48 chars at standard font ──────────
    // Most common ESC/POS 3-inch (80mm) printers print 48 chars/line
    // at normal size. 2-inch (58mm) printers print 32 chars/line.
    // If content was fitting in 2 inches before, width was set to 32.
    private val W = 48

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
        else null to "No printer selected. Go to Settings -> Select printer."
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

        val shopName    = AppPreferences.getShopName(context)
        val balance     = finalAmount - paidAmount
        val gstTotal    = items.sumOf { it.gstAmount }
        val grandTotal  = itemsTotal + gstTotal
        val prevBal     = finalAmount - grandTotal
        val billNo      = "B${System.currentTimeMillis().toString().takeLast(6)}"
        val isTamil     = AppLanguage.current == Language.TAMIL

        printer.print(buildReceipt(
            shopName    = shopName,
            billNo      = billNo,
            dateTime    = dateFormat.format(Date()),
            customer    = customer,
            items       = items,
            itemsTotal  = itemsTotal,
            gstTotal    = gstTotal,
            grandTotal  = grandTotal,
            prevBal     = prevBal,
            totalPayable= finalAmount,
            paidAmount  = paidAmount,
            balance     = balance,
            isTamil     = isTamil
        ))
        printer.disconnect()
        return if (isTamil) "வெற்றிகரமாக அச்சிடப்பட்டது" else "Printed successfully"
    }

    fun printTestBill(context: Context): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"
        val shopName = AppPreferences.getShopName(context)
        val isTamil  = AppLanguage.current == Language.TAMIL
        printer.print(buildTestReceipt(shopName, dateFormat.format(Date()), isTamil))
        printer.disconnect()
        return if (isTamil) "சோதனை அச்சு வெற்றி" else "Test print successful"
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
        prevBal: Double,
        totalPayable: Double,
        paidAmount: Double,
        balance: Double,
        isTamil: Boolean
    ): String {
        val sb = StringBuilder()

        // ── ESC/POS: initialize printer ───────────────────────────────
        sb.append("\u001B\u0040")           // ESC @ — reset
        sb.append("\u001B\u0061\u0001")     // ESC a 1 — center align

        // ── Header ────────────────────────────────────────────────────
        sb.append("\u001B\u0021\u0030")     // double width+height for shop name
        sb.append(shopName.uppercase())
        sb.append("\n")
        sb.append("\u001B\u0021\u0000")     // normal size

        if (isTamil) {
            sb.append("வரி விலைப்பட்டியல்\n")
        } else {
            sb.append("TAX INVOICE\n")
        }

        sb.append("\u001B\u0061\u0000")     // left align
        sb.append(divider())

        // ── Bill info ─────────────────────────────────────────────────
        if (isTamil) {
            sb.append(twoCol("பில் எண்: $billNo", dateTime))
        } else {
            sb.append(twoCol("Bill No: $billNo", dateTime))
        }
        sb.append(divider())

        // ── Customer ──────────────────────────────────────────────────
        val toLabel = if (isTamil) "பெறுநர்" else "To"
        sb.append("$toLabel: ${customer.name}\n")
        if (customer.phone.isNotBlank()) {
            val phLabel = if (isTamil) "தொ" else "Ph"
            sb.append("$phLabel: ${customer.phone}\n")
        }
        if (customer.address.isNotBlank()) {
            val addrLabel = if (isTamil) "முகவரி" else "Addr"
            sb.append("$addrLabel: ${customer.address.take(W - addrLabel.length - 2)}\n")
        }
        sb.append(divider())

        // ── Items header ──────────────────────────────────────────────
        val itemH   = if (isTamil) "பொருள்" else "Item"
        val qtyH    = if (isTamil) "அளவு" else "Qty"
        val priceH  = if (isTamil) "விலை" else "Price"
        val totalH  = if (isTamil) "மொத்தம்" else "Total"

        // Layout: item(20) qty(6) price(10) total(12) for 48-char width
        sb.append(String.format("%-20s %5s %9s %${W-36}s\n", itemH, qtyH, priceH, totalH))
        sb.append(thinDivider())

        // ── Items ─────────────────────────────────────────────────────
        items.forEach { item ->
            val lineTotal = item.price * item.quantity
            // Item name (wrap if > 20 chars)
            val nameParts = item.name.chunked(20)
            sb.append(String.format("%-20s %5s %9s %${W-36}s\n",
                nameParts[0],
                "${item.quantity}${item.unit.take(3)}",
                "Rs.${fmt(item.price)}",
                "Rs.${fmt(lineTotal)}"
            ))
            // Continuation lines for long names
            nameParts.drop(1).forEach { part ->
                sb.append(String.format("%-20s\n", "  $part"))
            }
            // GST line
            if (item.gstPercent > 0) {
                val gstLabel = if (isTamil) "  GST ${item.gstPercent.toInt()}%: Rs.${fmt(item.gstAmount)}"
                else         "  GST ${item.gstPercent.toInt()}%: Rs.${fmt(item.gstAmount)}"
                sb.append("$gstLabel\n")
            }
        }
        sb.append(thinDivider())

        // ── Totals ────────────────────────────────────────────────────
        val itemsTotalLabel = if (isTamil) "பொருட்கள் மொத்தம்" else "Items total"
        val gstTotalLabel   = if (isTamil) "GST மொத்தம்"       else "GST total"
        val grandTotalLabel = if (isTamil) "மொத்த தொகை"        else "Grand total"
        val prevBalLabel    = if (isTamil) "முந்தைய நிலுவை"   else "Previous due"
        val payableLabel    = if (isTamil) "செலுத்த வேண்டியது" else "Total payable"
        val paidLabel       = if (isTamil) "செலுத்தியது"       else "Paid"
        val balLabel        = if (isTamil) "நிலுவை"            else "Balance due"
        val paidFullLabel   = if (isTamil) "** முழுமையாக செலுத்தப்பட்டது **"
        else         "** PAID IN FULL **"

        sb.append(amtRow(itemsTotalLabel, "Rs.${fmt(itemsTotal)}"))
        if (gstTotal > 0.001) {
            sb.append(amtRow(gstTotalLabel, "Rs.${fmt(gstTotal)}"))
        }
        sb.append(amtRow(grandTotalLabel, "Rs.${fmt(grandTotal)}"))

        if (prevBal > 0.001) {
            sb.append(amtRow(prevBalLabel,  "Rs.${fmt(prevBal)}"))
            sb.append(amtRow(payableLabel,  "Rs.${fmt(totalPayable)}"))
        }

        sb.append(divider())
        sb.append(amtRow(paidLabel, "Rs.${fmt(paidAmount)}"))

        if (balance > 0.001) {
            sb.append(amtRow(balLabel, "Rs.${fmt(balance)}"))
        } else {
            sb.append("\u001B\u0061\u0001")  // center
            sb.append("$paidFullLabel\n")
            sb.append("\u001B\u0061\u0000")  // left
        }

        sb.append(divider())

        // ── Footer ────────────────────────────────────────────────────
        sb.append("\u001B\u0061\u0001")  // center
        val thankYou    = if (isTamil) "நன்றி! மீண்டும் வாருங்கள்" else "Thank You! Visit Again"
        val keepReceipt = if (isTamil) "இந்த ரசீதை பாதுகாக்கவும்" else "Keep this receipt safe"
        sb.append("$thankYou\n")
        sb.append("$keepReceipt\n")
        sb.append("\u001B\u0061\u0000")  // left

        // Feed and cut
        sb.append("\n\n\n")
        sb.append("\u001D\u0056\u0041\u0005")  // GS V A 5 — full cut with 5mm feed

        return sb.toString()
    }

    private fun buildTestReceipt(shopName: String, dateTime: String, isTamil: Boolean): String {
        val sb = StringBuilder()
        sb.append("\u001B\u0040")
        sb.append("\u001B\u0061\u0001")
        sb.append("\u001B\u0021\u0030")
        sb.append(shopName.uppercase()); sb.append("\n")
        sb.append("\u001B\u0021\u0000")
        if (isTamil) sb.append("வரி விலைப்பட்டியல்\n") else sb.append("TAX INVOICE\n")
        sb.append("\u001B\u0061\u0000")
        sb.append(divider())
        sb.append(twoCol(if (isTamil) "பில் எண்: T001" else "Bill No: T001", dateTime))
        sb.append(divider())
        sb.append(if (isTamil) "பெறுநர்: TEST CUSTOMER\n" else "To: TEST CUSTOMER\n")
        sb.append(if (isTamil) "தொ: 9999999999\n" else "Ph: 9999999999\n")
        sb.append(divider())
        sb.append(String.format("%-20s %5s %9s %12s\n",
            if (isTamil) "பொருள்" else "Item",
            if (isTamil) "அளவு" else "Qty",
            if (isTamil) "விலை" else "Price",
            if (isTamil) "மொத்தம்" else "Total"
        ))
        sb.append(thinDivider())
        sb.append(String.format("%-20s %5s %9s %12s\n", "Rice", "2Kg", "Rs.50.00", "Rs.100.00"))
        sb.append(String.format("%-20s %5s %9s %12s\n", "Oil", "1L", "Rs.100.00", "Rs.100.00"))
        sb.append("  GST 5%: Rs.5.00\n")
        sb.append(thinDivider())
        sb.append(amtRow(if (isTamil) "பொருட்கள் மொத்தம்" else "Items total", "Rs.200.00"))
        sb.append(amtRow("GST total", "Rs.5.00"))
        sb.append(amtRow(if (isTamil) "மொத்த தொகை" else "Grand total", "Rs.205.00"))
        sb.append(divider())
        sb.append(amtRow(if (isTamil) "செலுத்தியது" else "Paid", "Rs.205.00"))
        sb.append("\u001B\u0061\u0001")
        sb.append(if (isTamil) "** முழுமையாக செலுத்தப்பட்டது **\n"
        else         "** PAID IN FULL **\n")
        sb.append("\u001B\u0061\u0000")
        sb.append(divider())
        sb.append("\u001B\u0061\u0001")
        sb.append(if (isTamil) "நன்றி! மீண்டும் வாருங்கள்\n" else "Thank You! Visit Again\n")
        sb.append("\u001B\u0061\u0000")
        sb.append("\n\n\n")
        sb.append("\u001D\u0056\u0041\u0005")
        return sb.toString()
    }

    // ── Layout helpers — 48-char width ───────────────────────────────

    private fun divider() = "-".repeat(W) + "\n"
    private fun thinDivider() = "- ".repeat(W / 2) + "\n"

    private fun twoCol(left: String, right: String): String {
        val gap = W - left.length - right.length
        return if (gap > 0) left + " ".repeat(gap) + right + "\n"
        else left.take(W - right.length - 1) + " " + right + "\n"
    }

    private fun amtRow(label: String, value: String): String {
        val gap = W - label.length - value.length
        return if (gap > 0) label + " ".repeat(gap) + value + "\n"
        else label.take(W - value.length - 1) + " " + value + "\n"
    }

    private fun fmt(v: Double) = "%.2f".format(v)
}