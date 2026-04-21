package com.animan.wholesalemanager.utils

import java.math.BigDecimal
import java.math.RoundingMode

object PriceUtils {

    /** Round a Double to exactly 2 decimal places */
    fun Double.round2dp(): Double =
        BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** Format for display: 100.00, 80.53, 0.50 */
    fun Double.formatPrice(): String =
        "%.2f".format(BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble())

    /** Format with ₹ prefix */
    fun Double.toRupees(): String = "₹${"%.2f".format(round2dp())}"

    /** Format as whole rupees (for balance totals — no paise shown) */
    fun Double.toRupeesInt(): String = "₹${Math.round(this)}"

    /**
     * Parse a price string entered by the user.
     * - Rounds to 2dp immediately on input
     * - Returns null if blank/invalid
     * - Enforces max 2 decimal places
     */
    fun parseAndRound(input: String): Double? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val d = trimmed.toDoubleOrNull() ?: return null
        if (d < 0) return null
        return d.round2dp()
    }

    /**
     * Validate a price string as the user types.
     * Allows: digits, one dot, max 2 digits after dot.
     * Rejects: more than 2dp, negative sign, letters.
     */
    fun isValidPriceInput(input: String): Boolean {
        if (input.isEmpty()) return true
        val regex = Regex("""^\d+(\.\d{0,2})?$""")
        return regex.matches(input)
    }
}