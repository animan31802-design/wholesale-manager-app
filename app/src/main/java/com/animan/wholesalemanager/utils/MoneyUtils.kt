package com.animan.wholesalemanager.utils

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyUtils {
    /** Round to 2 decimal places — eliminates 0.200000000045 type noise */
    fun Double.roundMoney(): Double =
        BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** Format as whole rupees — ₹150 */
    fun Double.toRupees(): String = "₹${Math.round(this)}"

    /** Format with paise if non-zero — ₹150.50 or ₹150 */
    fun Double.toRupeesDetailed(): String {
        val rounded = BigDecimal(this).setScale(2, RoundingMode.HALF_UP)
        return if (rounded.scale() > 0 && rounded.remainder(BigDecimal.ONE) != BigDecimal.ZERO)
            "₹$rounded"
        else "₹${rounded.toInt()}"
    }
}