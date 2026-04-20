package com.animan.wholesalemanager.utils

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "app_prefs"

    private const val KEY_SHOP_NAME          = "shop_name"
    private const val KEY_BACKUP_ENABLED     = "backup_enabled"
    private const val KEY_BACKUP_FREQUENCY   = "backup_frequency"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getShopName(context: Context): String =
        prefs(context).getString(KEY_SHOP_NAME, "My Shop") ?: "My Shop"

    fun setShopName(context: Context, name: String) =
        prefs(context).edit().putString(KEY_SHOP_NAME, name).apply()

    fun isBackupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BACKUP_ENABLED, false)

    fun setBackupEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_BACKUP_ENABLED, enabled).apply()

    fun getBackupFrequency(context: Context): String =
        prefs(context).getString(KEY_BACKUP_FREQUENCY, "Manual only") ?: "Manual only"

    fun setBackupFrequency(context: Context, frequency: String) =
        prefs(context).edit().putString(KEY_BACKUP_FREQUENCY, frequency).apply()
}