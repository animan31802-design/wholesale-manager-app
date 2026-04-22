package com.animan.wholesalemanager.utils

import java.math.BigDecimal
import java.math.RoundingMode

object PriceUtils {

    fun Double.round2dp(): Double =
        BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** Always shows exactly 2 decimal places: 100.00, 80.53, 0.50 */
    fun Double.formatPrice(): String =
        "%.2f".format(BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble())

    /** ₹100.00 */
    fun Double.toRupees(): String = "₹${formatPrice()}"

    /** ₹100 (no paise — for balance totals where paise is noise) */
    fun Double.toRupeesInt(): String = "₹${Math.round(this)}"

    /** Validates price input as user types — allows digits + one dot + max 2dp */
    fun isValidPriceInput(input: String): Boolean {
        if (input.isEmpty()) return true
        return Regex("""^\d+(\.\d{0,2})?$""").matches(input)
    }

    fun parseAndRound(input: String): Double? {
        val d = input.trim().toDoubleOrNull() ?: return null
        if (d < 0) return null
        return d.round2dp()
    }
}