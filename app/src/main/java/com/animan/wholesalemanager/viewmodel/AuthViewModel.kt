package com.animan.wholesalemanager.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animan.wholesalemanager.data.local.DatabaseHelper
import com.animan.wholesalemanager.data.remote.FirebaseAuthHelper
import com.animan.wholesalemanager.utils.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {

    private val authHelper = FirebaseAuthHelper()

    var isLoading     = mutableStateOf(false)
    var isLoggingOut     = mutableStateOf(false)
    var errorMessage  = mutableStateOf<String?>(null)
    // Shown as a snackbar on the first screen after login/register
    var restoreStatus = mutableStateOf<String?>(null)

    // ── Login ─────────────────────────────────────────────────────────

    fun login(email: String, password: String, context: Context, onSuccess: () -> Unit) {
        if (email.isBlank())    { errorMessage.value = "Email cannot be empty";    return }
        if (password.isBlank()) { errorMessage.value = "Password cannot be empty"; return }
        isLoading.value = true
        authHelper.login(email, password,
            onSuccess = {
                isLoading.value = false
                //checkAndRestoreBackup(context)   // async — doesn't block navigation
                onSuccess()
            },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    // ── Register ──────────────────────────────────────────────────────

    fun register(email: String, password: String, context: Context, onSuccess: () -> Unit) {
        isLoading.value = true
        authHelper.register(email, password,
            onSuccess = {
                isLoading.value = false
                //checkAndRestoreBackup(context)   // async — new account may have prior backup
                onSuccess()
            },
            onError = { isLoading.value = false; errorMessage.value = it }
        )
    }

    // ── Logout with backup ────────────────────────────────────────────

    /**
     * Flow:
     * 1. If online → backup to Firebase (waits for completion)
     * 2. Clear all local SQLite data
     * 3. Firebase sign out
     * 4. Call onDone (caller should navigate to login and clear back-stack)
     *
     * Never blocks logout even if backup fails — data safety is best-effort.
     */
    fun logoutWithBackup(context: Context, onDone: () -> Unit) {
        if (isLoading.value) return
        isLoading.value = true
        viewModelScope.launch {
            if (isOnline(context)) {
                try {
                    BackupViewModel(context.applicationContext as Application)
                        .backupNowSuspend(context)
                } catch (_: Exception) { }
            }
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            withContext(Dispatchers.IO) {
                DatabaseHelper(context).clearAllData()
            }
            // Clear restore flag so next login triggers restore again
            if (uid != null) AppPreferences.clearRestoreDoneForUser(context, uid)
            authHelper.logout()
            isLoading.value = false
            onDone()
        }
    }

    // ── Restore after login ───────────────────────────────────────────

    /**
     * Called right after login/register success (fire-and-forget from the UI).
     * Checks Firebase for an existing backup and restores it if found.
     * Result is stored in [restoreStatus] — observe it on the Dashboard
     * to show a one-time snackbar.
     */
    fun checkAndRestoreBackup(context: Context, onRestoreComplete: () -> Unit = {}) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Already restored for this user session — skip
        if (AppPreferences.isRestoreDoneForUser(context, uid)) return

        isLoading.value = true
        viewModelScope.launch {
            val result = try {
                BackupViewModel(context.applicationContext as Application)
                    .restoreNowSuspend(context)
            } catch (e: Exception) {
                null
            }
            if (result != null &&
                !result.startsWith("No backup") &&
                !result.startsWith("Not logged")) {
                restoreStatus.value = result
                AppPreferences.setRestoreDoneForUser(context, uid)  // ← mark done
                onRestoreComplete()
            }
            isLoading.value = false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun isOnline(context: Context): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net  = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}