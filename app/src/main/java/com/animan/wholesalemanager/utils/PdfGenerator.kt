package com.animan.wholesalemanager.utils

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.animan.wholesalemanager.data.local.*

object PdfGenerator {

    fun generateBillPdf(
        context: Context,
        customer: Customer,
        items: List<BillItem>,
        total: Double,
        paid: Double,
        balance: Double
    ): File {

        val file = File(context.cacheDir, "bill_${System.currentTimeMillis()}.pdf")

        val writer = PdfWriter(FileOutputStream(file))
        val pdf = PdfDocument(writer)
        val document = Document(pdf)

        document.add(Paragraph("MY SHOP").setBold())
        document.add(Paragraph("-------------------------"))

        document.add(Paragraph("Customer: ${customer.name}"))
        document.add(Paragraph("-------------------------"))

        items.forEach {
            document.add(
                Paragraph("${it.name}  x${it.quantity}  ₹${it.price * it.quantity}")
            )
        }

        document.add(Paragraph("-------------------------"))

        document.add(Paragraph("Total: ₹$total"))
        document.add(Paragraph("Paid: ₹$paid"))
        document.add(Paragraph("Balance: ₹$balance"))

        document.add(Paragraph("-------------------------"))
        document.add(Paragraph("Thank You!"))

        document.close()

        return file
    }

    fun sharePdf(context: Context, file: File) {

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = android.content.Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(Intent.createChooser(intent, "Share Bill"))
    }
}