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
import com.animan.wholesalemanager.utils.TamilTransliterator
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager {

    private val printer    = BluetoothPrinter()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())

    // BluPrints BPMR3-BT — 80mm paper, 48 chars/line at Font A (normal)
    private val W = 48

    // ── ESC/POS constants ─────────────────────────────────────────────
    companion object {
        const val RESET         = "\u001B\u0040"
        const val ALIGN_CENTER  = "\u001B\u0061\u0001"
        const val ALIGN_LEFT    = "\u001B\u0061\u0000"
        const val SIZE_NORMAL   = "\u001B\u0021\u0000"
        const val SIZE_BOLD     = "\u001B\u0021\u0008"
        const val SIZE_BIG      = "\u001B\u0021\u0030"
        const val SIZE_BIG_BOLD = "\u001B\u0021\u0038"
        const val FONT_B        = "\u001B\u004D\u0001"
        const val FONT_A        = "\u001B\u004D\u0000"
        const val UNDERLINE_ON  = "\u001B\u002D\u0001"
        const val UNDERLINE_OFF = "\u001B\u002D\u0000"
        const val FULL_CUT      = "\u001D\u0056\u0041\u0005"
    }

    // ── Convenience: transliterate Tamil → Roman for printer output ───
    // Only converts if Tamil is present; English/numbers pass through unchanged.
    private fun p(text: String) = TamilTransliterator.forPrinter(text)

    // ── Device resolution ─────────────────────────────────────────────
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

    // ── Public print API ──────────────────────────────────────────────
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

        val shopName   = AppPreferences.getShopName(context)
        val balance    = finalAmount - paidAmount
        val gstTotal   = items.sumOf { it.gstAmount }
        val grandTotal = itemsTotal + gstTotal
        val prevBal    = finalAmount - grandTotal
        val billNo     = "B${System.currentTimeMillis().toString().takeLast(6)}"
        val isTamil    = AppLanguage.current == Language.TAMIL
        val style      = PrinterPreferences.getBillStyle(context)

        // ── Transliterate all user-supplied strings before building receipt ──
        // This ensures Tamil product names, customer names, addresses etc.
        // all print as readable Roman text instead of blank spaces.
        val safeShopName = p(shopName)
        val safeCustomer = customer.copy(
            name    = p(customer.name),
            phone   = customer.phone,           // phone is always ASCII
            address = p(customer.address)
        )
        val safeItems = items.map { it.copy(name = p(it.name), unit = p(it.unit)) }

        val receipt = when (style) {
            BillStyle.MINIMAL      -> buildMinimalReceipt(safeShopName, billNo,
                dateFormat.format(Date()), safeCustomer, safeItems,
                itemsTotal, gstTotal, grandTotal, prevBal,
                finalAmount, paidAmount, balance, isTamil)
            BillStyle.PROFESSIONAL -> buildProfessionalReceipt(safeShopName, billNo,
                dateFormat.format(Date()), safeCustomer, safeItems,
                itemsTotal, gstTotal, grandTotal, prevBal,
                finalAmount, paidAmount, balance, isTamil)
            BillStyle.GST_DETAILED -> buildGstDetailedReceipt(safeShopName, billNo,
                dateFormat.format(Date()), safeCustomer, safeItems,
                itemsTotal, gstTotal, grandTotal, prevBal,
                finalAmount, paidAmount, balance, isTamil)
        }

        printer.print(receipt)
        printer.disconnect()
        return if (isTamil) "Printed successfully" else "Printed successfully"
    }

    fun printTestBill(context: Context): String {
        val (device, error) = resolveDevice(context)
        if (device == null) return error
        if (!printer.connect(device)) return "Printer connection failed"

        val shopName = p(AppPreferences.getShopName(context))
        val isTamil  = AppLanguage.current == Language.TAMIL
        val style    = PrinterPreferences.getBillStyle(context)

        val testItems = listOf(
            BillItem("1", "Rice (1kg)",         50.0, 40.0, "Kg",  2.0, 5.0),
            BillItem("2", "Sunflower Oil (1L)", 120.0, 95.0, "L",   1.0, 5.0),
            BillItem("3", "Sugar (1kg)",         45.0, 35.0, "Kg",  1.0, 0.0),
            BillItem("4", "Tea Powder (250g)",   35.0, 28.0, "Pcs", 1.0, 0.0),
            BillItem("5", "Bread",               25.0, 20.0, "Pcs", 1.0, 0.0)
        )
        val itemsTotal = testItems.sumOf { it.price * it.quantity }
        val gstTotal   = testItems.sumOf { it.gstAmount }
        val grandTotal = itemsTotal + gstTotal
        val customer   = Customer("", "Test Customer", "9999999999",
            "123, Main Street, Chennai", null, null)

        val receipt = when (style) {
            BillStyle.MINIMAL      -> buildMinimalReceipt(shopName, "T001",
                dateFormat.format(Date()), customer, testItems,
                itemsTotal, gstTotal, grandTotal, 0.0,
                grandTotal, grandTotal, 0.0, isTamil)
            BillStyle.PROFESSIONAL -> buildProfessionalReceipt(shopName, "T001",
                dateFormat.format(Date()), customer, testItems,
                itemsTotal, gstTotal, grandTotal, 0.0,
                grandTotal, grandTotal, 0.0, isTamil)
            BillStyle.GST_DETAILED -> buildGstDetailedReceipt(shopName, "T001",
                dateFormat.format(Date()), customer, testItems,
                itemsTotal, gstTotal, grandTotal, 0.0,
                grandTotal, grandTotal, 0.0, isTamil)
        }
        printer.print(receipt)
        printer.disconnect()
        return "Test print successful"
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STYLE 1 — MINIMAL
    // ═══════════════════════════════════════════════════════════════════
    private fun buildMinimalReceipt(
        shopName: String, billNo: String, dateTime: String, customer: Customer,
        items: List<BillItem>, itemsTotal: Double, gstTotal: Double, grandTotal: Double,
        prevBal: Double, totalPayable: Double, paidAmount: Double, balance: Double,
        isTamil: Boolean
    ): String = buildString {

        append(RESET)
        append(ALIGN_CENTER)
        append(SIZE_BIG_BOLD); append(shopName.uppercase()); append("\n")
        append(SIZE_NORMAL)
        append(ALIGN_LEFT)
        append(dashDivider())

        append("Bill No     : $billNo\n")
        append("Date & Time : $dateTime\n")
        append(dashDivider())

        append("To   : ${customer.name}\n")
        if (customer.phone.isNotBlank()) append("Ph   : ${customer.phone}\n")
        if (customer.address.isNotBlank())
            wrapField("Addr : ", customer.address, 7).forEach { append(it) }
        append(dashDivider())

        append(SIZE_BOLD)
        append(String.format("%-22s %5s %${W - 28}s\n", "ITEM", "QTY", "TOTAL"))
        append(SIZE_NORMAL)
        append(thinDotDivider())

        items.forEach { item ->
            val lineTotal = item.price * item.quantity
            val qtyStr    = "${item.quantity}${item.unit.take(3)}"
            val totalStr  = fmt(lineTotal)

            if (item.name.length <= 22) {
                append(String.format("%-22s %5s %${W - 28}s\n", item.name, qtyStr, totalStr))
            } else {
                val chunks = item.name.chunked(22)
                chunks.dropLast(1).forEach { append(String.format("%-22s\n", it)) }
                append(String.format("%-22s %5s %${W - 28}s\n",
                    chunks.last().take(22), qtyStr, totalStr))
            }
            append(FONT_B)
            append("  @ Rs.${fmt(item.price)}/unit")
            if (item.gstPercent > 0) append("  GST ${item.gstPercent.toInt()}%: Rs.${fmt(item.gstAmount)}")
            append("\n")
            append(FONT_A)
        }
        append(dashDivider())

        append(amtRow("SUBTOTAL", fmt(itemsTotal)))
        if (gstTotal > 0.001) append(amtRow("GST", fmt(gstTotal)))
        append(SIZE_BOLD)
        append(amtRow("TOTAL AMOUNT", fmt(grandTotal)))
        append(SIZE_NORMAL)
        append(dashDivider())

        if (prevBal > 0.001) append(amtRow("Previous due", fmt(prevBal)))
        append(amtRow("AMOUNT PAID", fmt(paidAmount)))
        if (balance > 0.001) {
            append(SIZE_BOLD); append(UNDERLINE_ON)
            append(amtRow("BALANCE", fmt(balance)))
            append(UNDERLINE_OFF); append(SIZE_NORMAL)
        }
        append(dashDivider())

        append(ALIGN_CENTER)
        if (balance <= 0.001) {
            append(SIZE_BOLD); append("** PAID IN FULL **\n"); append(SIZE_NORMAL)
        }
        append(FONT_B)
        append("--- Thank You! ---\n")
        append("Visit Again!\n")
        append(FONT_A)
        append(ALIGN_LEFT)
        append(dashDivider())
        append("\n\n\n"); append(FULL_CUT)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STYLE 2 — PROFESSIONAL
    // ═══════════════════════════════════════════════════════════════════
    private fun buildProfessionalReceipt(
        shopName: String, billNo: String, dateTime: String, customer: Customer,
        items: List<BillItem>, itemsTotal: Double, gstTotal: Double, grandTotal: Double,
        prevBal: Double, totalPayable: Double, paidAmount: Double, balance: Double,
        isTamil: Boolean
    ): String = buildString {

        append(RESET)
        append(ALIGN_CENTER)
        append(SIZE_BIG_BOLD); append(shopName.uppercase()); append("\n")
        append(SIZE_BOLD);     append("BILL\n")
        append(SIZE_NORMAL)
        append(ALIGN_LEFT)
        append(solidDivider())

        append(twoCol("Bill No. : $billNo", dateTime))
        append(solidDivider())

        append(SIZE_BOLD); append("To : ${customer.name}\n"); append(SIZE_NORMAL)
        if (customer.phone.isNotBlank()) append("Ph : ${customer.phone}\n")
        if (customer.address.isNotBlank())
            wrapField("Ad : ", customer.address, 5).forEach { append(it) }
        append(solidDivider())

        val c1 = 20; val c2 = 4; val c3 = 9; val c4 = W - c1 - c2 - c3 - 3
        append(SIZE_BOLD)
        append(String.format("%-${c1}s %${c2}s %${c3}s %${c4}s\n",
            "ITEM", "QTY", "PRICE", "TOTAL"))
        append(SIZE_NORMAL)
        append(thinSolidDivider())

        items.forEach { item ->
            val lineTotal = item.price * item.quantity
            val qtyStr    = "${item.quantity}${item.unit.take(3)}"
            val priceStr  = "Rs.${fmt(item.price)}"
            val totalStr  = "Rs.${fmt(lineTotal)}"

            if (item.name.length <= c1) {
                append(String.format("%-${c1}s %${c2}s %${c3}s %${c4}s\n",
                    item.name, qtyStr, priceStr, totalStr))
            } else {
                val chunks = item.name.chunked(c1)
                append(String.format("%-${c1}s %${c2}s %${c3}s %${c4}s\n",
                    chunks[0], qtyStr, priceStr, totalStr))
                chunks.drop(1).forEach { append(String.format("  %-${c1 - 2}s\n", it)) }
            }
            if (item.gstPercent > 0) {
                append(FONT_B)
                append("  GST ${item.gstPercent.toInt()}% : Rs.${fmt(item.gstAmount)}\n")
                append(FONT_A)
            }
            append(thinSolidDivider())
        }

        append(amtRow("SUBTOTAL", fmt(itemsTotal)))
        if (gstTotal > 0.001) append(amtRow("GST", fmt(gstTotal)))
        append(SIZE_BOLD)
        append(amtRow("TOTAL AMOUNT", fmt(grandTotal)))
        append(SIZE_NORMAL)
        append(solidDivider())

        if (prevBal > 0.001) {
            append(amtRow("Previous due", fmt(prevBal)))
            append(SIZE_BOLD); append(amtRow("Total payable", fmt(totalPayable))); append(SIZE_NORMAL)
            append(solidDivider())
        }

        append(amtRow("AMOUNT PAID", fmt(paidAmount)))
        if (balance > 0.001) {
            append(SIZE_BOLD); append(UNDERLINE_ON)
            append(amtRow("BALANCE", fmt(balance)))
            append(UNDERLINE_OFF); append(SIZE_NORMAL)
        }
        append(solidDivider())

        append(ALIGN_CENTER)
        if (balance <= 0.001) {
            append(SIZE_BOLD); append("** PAID IN FULL **\n"); append(SIZE_NORMAL)
        }
        append("-- Thank You! --\n")
        append("Visit Again!\n")
        append(ALIGN_LEFT)
        append(dashDivider())
        append("\n\n\n"); append(FULL_CUT)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  STYLE 3 — GST DETAILED
    // ═══════════════════════════════════════════════════════════════════
    private fun buildGstDetailedReceipt(
        shopName: String, billNo: String, dateTime: String, customer: Customer,
        items: List<BillItem>, itemsTotal: Double, gstTotal: Double, grandTotal: Double,
        prevBal: Double, totalPayable: Double, paidAmount: Double, balance: Double,
        isTamil: Boolean
    ): String = buildString {

        append(RESET)
        append(ALIGN_CENTER)
        append(SIZE_BIG_BOLD); append(shopName.uppercase()); append("\n")
        append(SIZE_BOLD);     append("TAX INVOICE\n")
        append(SIZE_NORMAL)
        append(ALIGN_LEFT)
        append(solidDivider())

        append(FONT_B)
        append("Bill No   : $billNo\n")
        append("Date/Time : $dateTime\n")
        append(FONT_A)
        append(solidDivider())

        append(SIZE_BOLD); append("Billed To :\n"); append(SIZE_NORMAL)
        append("  ${customer.name}\n")
        if (customer.phone.isNotBlank()) append("  Ph : ${customer.phone}\n")
        if (customer.address.isNotBlank())
            customer.address.chunked(W - 4).forEach { append("  $it\n") }
        append(solidDivider())

        val sW = 2; val nW = 17; val qW = 4; val rW = 7; val aW = W - sW - nW - qW - rW - 4
        append(SIZE_BOLD)
        append(String.format("%${sW}s %-${nW}s %${qW}s %${rW}s %${aW}s\n",
            "#", "ITEM", "QTY", "RATE", "AMT"))
        append(SIZE_NORMAL)
        append(thinSolidDivider())

        items.forEachIndexed { idx, item ->
            val lineAmt = item.price * item.quantity
            val qtyStr  = "${item.quantity}${item.unit.take(3)}"

            if (item.name.length <= nW) {
                append(String.format("%${sW}d %-${nW}s %${qW}s %${rW}s %${aW}s\n",
                    idx + 1, item.name, qtyStr, fmt(item.price), fmt(lineAmt)))
            } else {
                val chunks = item.name.chunked(nW)
                append(String.format("%${sW}d %-${nW}s %${qW}s %${rW}s %${aW}s\n",
                    idx + 1, chunks[0], qtyStr, fmt(item.price), fmt(lineAmt)))
                chunks.drop(1).forEach { append(String.format("   %-${nW}s\n", it)) }
            }
            if (item.gstPercent > 0) {
                val halfPct = item.gstPercent / 2
                val halfAmt = item.gstAmount / 2
                append(FONT_B)
                append("   CGST ${fmt(halfPct)}%:${fmt(halfAmt)}  SGST ${fmt(halfPct)}%:${fmt(halfAmt)}\n")
                append(FONT_A)
            }
        }
        append(thinSolidDivider())

        val cgst = gstTotal / 2; val sgst = gstTotal / 2
        append(amtRow("Taxable amount", fmt(itemsTotal)))
        if (gstTotal > 0.001) {
            append(FONT_B)
            append(amtRow("  CGST", fmt(cgst)))
            append(amtRow("  SGST", fmt(sgst)))
            append(FONT_A)
            append(amtRow("Total GST", fmt(gstTotal)))
        }
        append(SIZE_BOLD); append(amtRow("GRAND TOTAL", fmt(grandTotal))); append(SIZE_NORMAL)
        append(solidDivider())

        if (prevBal > 0.001) {
            append(amtRow("Previous due", fmt(prevBal)))
            append(SIZE_BOLD); append(amtRow("Total payable", fmt(totalPayable))); append(SIZE_NORMAL)
            append(solidDivider())
        }

        append(amtRow("AMOUNT PAID", fmt(paidAmount)))
        if (balance > 0.001) {
            append(SIZE_BOLD); append(UNDERLINE_ON)
            append(amtRow("BALANCE DUE", fmt(balance)))
            append(UNDERLINE_OFF); append(SIZE_NORMAL)
        } else {
            append(ALIGN_CENTER)
            append(SIZE_BOLD); append("** PAID IN FULL **\n"); append(SIZE_NORMAL)
            append(ALIGN_LEFT)
        }
        append(solidDivider())

        append(ALIGN_CENTER)
        append(SIZE_BOLD); append("Thank You! Visit Again\n"); append(SIZE_NORMAL)
        append(FONT_B); append("Computer generated invoice\n"); append(FONT_A)
        append(ALIGN_LEFT)
        append(dashDivider())
        append("\n\n\n"); append(FULL_CUT)
    }

    // ── Layout helpers ────────────────────────────────────────────────

    private fun wrapField(prefix: String, value: String, prefixLen: Int): List<String> {
        val maxW = W - prefixLen
        if (value.length <= maxW) return listOf("$prefix$value\n")
        val result = mutableListOf<String>()
        val chunks = value.chunked(maxW)
        result.add("$prefix${chunks[0]}\n")
        chunks.drop(1).forEach { result.add(" ".repeat(prefixLen) + "$it\n") }
        return result
    }

    private fun solidDivider()     = "-".repeat(W) + "\n"
    private fun thinSolidDivider() = "- ".repeat(W / 2) + "\n"
    private fun dashDivider()      = "- ".repeat(W / 2) + "\n"
    private fun thinDotDivider()   = ". ".repeat(W / 2) + "\n"

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