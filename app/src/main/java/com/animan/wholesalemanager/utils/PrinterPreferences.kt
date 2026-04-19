package com.animan.wholesalemanager.utils

import android.content.Context

object PrinterPreferences {
    private const val PREF_NAME = "printer_prefs"
    private const val KEY_ADDRESS = "selected_printer_address"

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
}