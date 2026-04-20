package com.animan.wholesalemanager.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object UpiQrGenerator {

    /**
     * Generates a UPI deep-link QR code bitmap.
     *
     * UPI URL format:
     *   upi://pay?pa=VPA&pn=NAME&am=AMOUNT&cu=INR&tn=NOTE
     *
     * pa  = payee VPA (UPI ID)  e.g. yourshop@upi
     * pn  = payee name          e.g. My Shop
     * am  = amount              e.g. 500.00
     * cu  = currency            always INR
     * tn  = transaction note    e.g. Invoice #AB12CD34
     */
    fun generate(
        upiId: String,
        payeeName: String,
        amount: Double,
        note: String,
        sizePx: Int = 512
    ): Bitmap? {
        if (upiId.isBlank()) return null

        val upiUrl = buildString {
            append("upi://pay")
            append("?pa=${upiId.trim()}")
            append("&pn=${payeeName.trim().replace(" ", "%20")}")
            append("&am=${"%.2f".format(amount)}")
            append("&cu=INR")
            if (note.isNotBlank())
                append("&tn=${note.trim().replace(" ", "%20")}")
        }

        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(upiUrl, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}