package com.animan.wholesalemanager.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.animan.wholesalemanager.utils.BluetoothPrinter
import com.animan.wholesalemanager.utils.PrinterPreferences

/**
 * Tamil font size test printer.
 *
 * Your printer: BluPrints BPMR3-BT
 * Paper width  : 3 inch = 576 dots at 203 DPI
 * Method       : Bitmap image printing via ESC/POS GS v 0
 *
 * ESC/POS image command:
 *   GS v 0  (raster bit image)
 *   Format: 1D 76 30 m xL xH yL yH [data]
 *   m = 0  → normal density
 *   xL xH = (width in bytes) LSB MSB
 *   yL yH = (height in lines) LSB MSB
 *   data  = 1 bit per pixel, 8 pixels per byte, row by row
 */
class TamilTestPrinter {

    private val printer     = BluetoothPrinter()
    private val PRINTER_WIDTH_PX = 576   // 3-inch at 203 DPI

    // Tamil test string — covers vowels, consonants, matras, numbers
    private val TEST_TEXT = "இணை எழுத்து"

    // All sizes to test — from minimum visible to maximum useful
    private val TEST_SIZES = listOf(
        8f   to "8sp  — Too small",
        10f  to "10sp — Very small",
        12f  to "12sp — Small",
        14f  to "14sp — Minimum acceptable",
        16f  to "16sp — Good compact",
        18f  to "18sp — Good clear",
        20f  to "20sp — Recommended",
        22f  to "22sp — Large emphasis",
        24f  to "24sp — Very large",
        26f  to "26sp — Extra large heading",
        28f  to "28sp — Big heading",
        32f  to "32sp — Title",
        36f  to "36sp — Display",
        40f  to "40sp — Maximum useful"
    )

    /**
     * Prints a Tamil font size test page on the paired printer.
     * Each size prints:
     *   - The size label in English (ESC/POS text)
     *   - The Tamil test text as a bitmap at that size
     *
     * Returns a result message.
     */
    fun printTamilFontTest(context: Context): String {
        val savedAddress = PrinterPreferences.getSavedAddress(context)
            ?: return "No printer selected. Go to Settings → Select printer."

        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            ?: return "Bluetooth not available"

        if (!adapter.isEnabled) return "Bluetooth is not enabled"

        val device = try {
            adapter.bondedDevices?.firstOrNull { it.address == savedAddress }
        } catch (e: SecurityException) {
            return "Bluetooth permission denied"
        } ?: return "Saved printer not found. Re-select in Settings."

        if (!printer.connect(device)) return "Printer connection failed"
        printer.initialize()

        return try {
            val sb = StringBuilder()

            // ── Header ────────────────────────────────────────────────
            sb.append("\u001B\u0040")              // RESET
            sb.append("\u001B\u0061\u0001")        // CENTER
            sb.append("\u001B\u0021\u0038")        // SIZE_BIG_BOLD
            sb.append("TAMIL FONT SIZE TEST\n")
            sb.append("\u001B\u0021\u0000")        // SIZE_NORMAL
            sb.append("BluPrints BPMR3-BT | 3 inch\n")
            sb.append("Text: \"$TEST_TEXT\"\n")
            sb.append("\u001B\u0061\u0000")        // LEFT
            sb.append("-".repeat(32) + "\n")

            // Print the header text first
            printer.print(sb.toString())

            // ── Print each size as bitmap ─────────────────────────────
            TEST_SIZES.forEach { (size, label) ->
                // 1. Print the size label as ESC/POS text
                val labelStr = buildString {
                    append("\u001B\u0021\u0000")   // normal
                    append("$label\n")
                }
                printer.print(labelStr)

                // 2. Render Tamil text to bitmap at this size
                val bitmap = renderTamilBitmap(context, TEST_TEXT + "jhgjhgjhgfj" + TEST_TEXT, size)

                // 3. Send bitmap as ESC/POS raster image
                val imageBytes = bitmapToEscPos(bitmap)
                printer.printRaw(imageBytes)

                // 4. Small gap between entries
                printer.print("\n")
            }

            // ── Footer ────────────────────────────────────────────────
            printer.print(buildString {
                append("-".repeat(32) + "\n")
                append("\u001B\u0061\u0001")       // CENTER
                append("End of font test\n")
                append("\u001B\u0061\u0000")       // LEFT
                append("\n\n\n")
                append("\u001D\u0056\u0041\u0005") // FULL_CUT
            })

            "Tamil font test printed successfully"
        } catch (e: Exception) {
            "Print error: ${e.message}"
        } finally {
            printer.disconnect()
        }
    }

    /**
     * Renders Tamil text to a 1-bit Bitmap at the given text size.
     * Width is fixed to PRINTER_WIDTH_PX.
     * Height is auto-calculated to fit the text.
     */
    private fun renderTamilBitmap(
        context : Context,
        text    : String,
        textSize: Float
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color         = Color.BLACK
            this.textSize = textSize
            typeface      = getBestTamilTypeface(context)
            isAntiAlias   = true
        }

        // Measure text height
        val fm     = paint.fontMetrics
        val lineH  = (fm.descent - fm.ascent + fm.leading + 4).toInt()
        val height = lineH + 8   // padding top/bottom

        val bitmap = Bitmap.createBitmap(PRINTER_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Draw text — left-aligned with 4px margin
        canvas.drawText(text, 4f, -fm.ascent + 4f, paint)

        return bitmap
    }

    /**
     * Converts a Bitmap to ESC/POS GS v 0 raster image bytes.
     * Each pixel → 1 bit. Row width padded to nearest byte.
     * Command: 1D 76 30 00 xL xH yL yH (data...)
     */
    private fun bitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width      = bitmap.width
        val height     = bitmap.height
        val widthBytes = (width + 7) / 8   // bytes per row (rounded up)

        val data = ArrayList<Byte>()

        // GS v 0 header
        data.add(0x1D.toByte())
        data.add(0x76.toByte())
        data.add(0x30.toByte())
        data.add(0x00.toByte())   // m = normal density

        // xL xH = widthBytes
        data.add((widthBytes and 0xFF).toByte())
        data.add(((widthBytes shr 8) and 0xFF).toByte())

        // yL yH = height
        data.add((height and 0xFF).toByte())
        data.add(((height shr 8) and 0xFF).toByte())

        // Pixel data — 1 bit per pixel, MSB first
        for (y in 0 until height) {
            for (byteX in 0 until widthBytes) {
                var byte = 0
                for (bit in 0..7) {
                    val x = byteX * 8 + bit
                    if (x < width) {
                        val pixel   = bitmap.getPixel(x, y)
                        val r       = (pixel shr 16) and 0xFF
                        val g       = (pixel shr 8) and 0xFF
                        val b       = pixel and 0xFF
                        val lum     = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        val isDark  = lum < 128
                        if (isDark) byte = byte or (0x80 shr bit)
                    }
                }
                data.add(byte.toByte())
            }
        }

        return data.toByteArray()
    }

    /**
     * Returns the best available Tamil-capable typeface.
     * Priority:
     * 1. NotoSansTamil if available in assets
     * 2. System default (may or may not support Tamil on device)
     */
    private fun getBestTamilTypeface(context: Context): Typeface {
        // Try to load NotoSansTamil from assets if you've added it
        // Add NotoSansTamil-Regular.ttf to app/src/main/assets/fonts/
        return try {
            Typeface.createFromAsset(context.assets, "fonts/NotoSansTamil-Regular.ttf")
        } catch (_: Exception) {
            // Fall back to system default — works on most Android 8+ devices
            // which include Noto Sans Tamil in the system font
            Typeface.DEFAULT
        }
    }
}