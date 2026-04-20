package com.animan.wholesalemanager.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.animan.wholesalemanager.data.local.BillItem
import com.animan.wholesalemanager.data.local.Customer
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generateBillPdf(
        context: Context,
        customer: Customer,
        items: List<BillItem>,
        itemsTotal: Double,
        gstTotal: Double,
        paid: Double,
        balance: Double
    ): File {
        val shopName = AppPreferences.getShopName(context)
        val file = File(context.cacheDir, "bill_${System.currentTimeMillis()}.pdf")
        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val doc = Document(pdf)

        // Use Helvetica (built-in) — iText7 built-in fonts handle Latin chars fine.
        // For the Rs. symbol we write "Rs." as text since iText7 standard fonts
        // don't embed Devanagari/rupee glyphs. The receipt printer uses raw bytes
        // so that path is fine; PDF uses "Rs." as the universally readable fallback.
        val bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val normal = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date())

        // Header
        doc.add(Paragraph(shopName).setFont(bold).setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("Tax Invoice").setFont(normal).setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("Date: $dateStr").setFont(normal).setFontSize(9f))
        doc.add(Paragraph("Customer: ${customer.name}").setFont(bold).setFontSize(11f))
        if (customer.phone.isNotBlank())
            doc.add(Paragraph("Phone: ${customer.phone}").setFont(normal).setFontSize(9f))

        doc.add(Paragraph(" "))

        // Items table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 15f, 15f, 15f, 15f)))
            .useAllAvailableWidth()

        listOf("Item", "Qty", "Price", "GST", "Total").forEach { heading ->
            table.addHeaderCell(Cell().add(Paragraph(heading).setFont(bold).setFontSize(9f)))
        }

        items.forEach { item ->
            val lineTotal = item.price * item.quantity
            table.addCell(Cell().add(Paragraph(item.name).setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("${item.quantity} ${item.unit}").setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("Rs.${item.price.toInt()}").setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(
                if (item.gstPercent > 0) "${item.gstPercent.toInt()}%" else "-")
                .setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("Rs.${item.totalWithGst.toInt()}").setFont(normal).setFontSize(9f)))
        }
        doc.add(table)
        doc.add(Paragraph(" "))

        // Totals
        fun addTotalRow(label: String, value: String) =
            doc.add(Paragraph("$label: $value")
                .setFont(normal).setFontSize(10f)
                .setTextAlignment(TextAlignment.RIGHT))

        addTotalRow("Items total", "Rs.${itemsTotal.toInt()}")
        if (gstTotal > 0) addTotalRow("GST", "Rs.${gstTotal.toInt()}")
        addTotalRow("Grand total", "Rs.${(itemsTotal + gstTotal).toInt()}")
        addTotalRow("Paid", "Rs.${paid.toInt()}")
        if (balance > 0) addTotalRow("Balance due", "Rs.${balance.toInt()}")

        doc.add(Paragraph(" "))
        doc.add(Paragraph("Thank you for your business!")
            .setFont(bold).setFontSize(10f).setTextAlignment(TextAlignment.CENTER))

        doc.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share bill"))
    }
}