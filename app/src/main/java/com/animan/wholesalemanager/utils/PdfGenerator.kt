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
import com.animan.wholesalemanager.utils.PriceUtils.formatPrice
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
        val S        = AppLanguage.strings
        val shopName = AppPreferences.getShopName(context)
        val file     = File(context.cacheDir, "bill_${System.currentTimeMillis()}.pdf")
        val writer   = PdfWriter(FileOutputStream(file))
        val pdf      = PdfDocument(writer)
        val doc      = Document(pdf)

        val bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)
        val normal = PdfFontFactory.createFont(StandardFonts.HELVETICA)

        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

        doc.add(Paragraph(shopName).setFont(bold).setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph(S.invoice).setFont(normal).setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER))
        doc.add(Paragraph("${S.date}: $dateStr").setFont(normal).setFontSize(9f))
        doc.add(Paragraph("${S.customer}: ${customer.name}").setFont(bold).setFontSize(11f))
        if (customer.phone.isNotBlank())
            doc.add(Paragraph("${S.phone}: ${customer.phone}").setFont(normal).setFontSize(9f))
        doc.add(Paragraph(" "))

        val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 12f, 16f, 12f, 20f)))
            .useAllAvailableWidth()

        listOf(S.itemLabel, S.qtyLabel, S.priceLabel, S.gstLabel, S.totalLabel).forEach { h ->
            table.addHeaderCell(Cell().add(Paragraph(h).setFont(bold).setFontSize(9f)))
        }

        items.forEach { item ->
            table.addCell(Cell().add(Paragraph(item.name).setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("${item.quantity} ${item.unit}").setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("Rs.${item.price.formatPrice()}").setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph(
                if (item.gstPercent > 0) "${item.gstPercent.toInt()}%" else "-")
                .setFont(normal).setFontSize(9f)))
            table.addCell(Cell().add(Paragraph("Rs.${item.totalWithGst.formatPrice()}").setFont(normal).setFontSize(9f)))
        }
        doc.add(table)
        doc.add(Paragraph(" "))

        fun addRow(label: String, value: String) =
            doc.add(Paragraph("$label: $value").setFont(normal).setFontSize(10f)
                .setTextAlignment(TextAlignment.RIGHT))

        addRow(S.itemsTotal, "Rs.${itemsTotal.formatPrice()}")
        if (gstTotal > 0) addRow(S.gstLabel, "Rs.${gstTotal.formatPrice()}")
        addRow(S.grandTotal, "Rs.${(itemsTotal + gstTotal).formatPrice()}")
        addRow(S.paidLabel, "Rs.${paid.formatPrice()}")
        if (balance > 0) addRow(S.balanceLabel, "Rs.${balance.formatPrice()}")

        doc.add(Paragraph(" "))
        doc.add(Paragraph(S.thankYou).setFont(bold).setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER))
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