package com.animan.wholesalemanager.utils

import android.content.Context
import com.animan.wholesalemanager.printer.BillStyle

object PrinterPreferences {
    private const val PREF_NAME = "printer_prefs"
    private const val KEY_ADDRESS = "printer_address"

    private const val PREF_FILE     = "printer_prefs"
    private const val KEY_BILL_STYLE = "bill_style"

    fun saveAddress(context: Context, address: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ADDRESS, address).apply()
    }

    fun getSavedAddress(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS, null)
    }

    fun clearSavedAddress(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ADDRESS).apply()
    }

    fun getBillStyle(context: Context): BillStyle {
        val name = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getString(KEY_BILL_STYLE, BillStyle.PROFESSIONAL.name)
        return runCatching { BillStyle.valueOf(name!!) }.getOrDefault(BillStyle.PROFESSIONAL)
    }

    fun setBillStyle(context: Context, style: BillStyle) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_BILL_STYLE, style.name).apply()
}