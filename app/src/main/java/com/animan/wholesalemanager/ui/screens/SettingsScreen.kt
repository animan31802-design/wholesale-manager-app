package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.utils.Language
import com.animan.wholesalemanager.utils.UpiQrGenerator
import com.animan.wholesalemanager.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val S        = AppLanguage.strings
    val backupViewModel: BackupViewModel = viewModel()

    var shopName        by remember { mutableStateOf(AppPreferences.getShopName(context)) }
    var upiId           by remember { mutableStateOf(AppPreferences.getUpiId(context)) }
    var backupEnabled   by remember { mutableStateOf(AppPreferences.isBackupEnabled(context)) }
    var backupFrequency by remember { mutableStateOf(AppPreferences.getBackupFrequency(context)) }
    var shopNameSaved   by remember { mutableStateOf("") }
    var upiSaved        by remember { mutableStateOf("") }

    // Preview QR for the saved UPI ID
    val previewQr = remember(upiId) {
        if (upiId.isNotBlank())
            UpiQrGenerator.generate(upiId, shopName, 1.0, "Test", 256)
        else null
    }

    val frequencyOptions = listOf("Manual only", "Daily", "Weekly")

    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Shop name ─────────────────────────────────────────────
            SectionHeader(Icons.Filled.Store, "Shop details")

            OutlinedTextField(value = shopName, onValueChange = { shopName = it; shopNameSaved = "" },
                label = { Text("Shop name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (shopNameSaved.isNotEmpty())
                Text(shopNameSaved, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)

            Button(onClick = {
                AppPreferences.setShopName(context, shopName.trim())
                shopNameSaved = "Saved."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save shop name") }

            HorizontalDivider()

            // ── UPI ID ────────────────────────────────────────────────
            SectionHeader(Icons.Filled.QrCode, "UPI payment")

            Text(
                "Enter your UPI ID so customers can pay by scanning a QR on the bill screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it; upiSaved = "" },
                label = { Text("UPI ID (e.g. yourshop@upi)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("yourshop@okicici") }
            )

            if (upiSaved.isNotEmpty())
                Text(upiSaved, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)

            Button(onClick = {
                AppPreferences.setUpiId(context, upiId.trim())
                upiSaved = "UPI ID saved."
            }, modifier = Modifier.fillMaxWidth()) { Text("Save UPI ID") }

            // Show a preview QR with amount ₹1 so owner can verify it works
            if (previewQr != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Preview QR (amount: ₹1 — for testing only)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Image(bitmap = previewQr.asImageBitmap(),
                        contentDescription = "UPI QR preview",
                        modifier = Modifier.size(160.dp))
                }
            }

            HorizontalDivider()

            // ── Language ──────────────────────────────────────────────
            SectionHeader(Icons.Filled.Language, S.language)

            Text(
                "Current: ${if (AppLanguage.current == Language.TAMIL) "தமிழ்" else "English"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        AppLanguage.setLanguage(context, Language.ENGLISH)
                        // Restart activity to re-render all composables with new strings
                        (context as? android.app.Activity)?.recreate()
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (AppLanguage.current == Language.ENGLISH)
                        ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("English") }

                Button(
                    onClick = {
                        AppLanguage.setLanguage(context, Language.TAMIL)
                        (context as? android.app.Activity)?.recreate()
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (AppLanguage.current == Language.TAMIL)
                        ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("தமிழ்") }
            }

            HorizontalDivider()

            // ── Printer ───────────────────────────────────────────────
            SectionHeader(Icons.Filled.Print, "Printer")

            OutlinedButton(onClick = { navController.navigate("printer_selector") },
                modifier = Modifier.fillMaxWidth()) {
                Text("Select / change Bluetooth printer")
            }

            HorizontalDivider()

            // ── Backup ────────────────────────────────────────────────
            SectionHeader(Icons.Filled.CloudUpload, "Cloud backup")

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Enable automatic backup")
                    Text("Backs up to Firebase", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = backupEnabled, onCheckedChange = {
                    backupEnabled = it; AppPreferences.setBackupEnabled(context, it)
                })
            }

            if (backupEnabled) {
                Text("Backup frequency", style = MaterialTheme.typography.labelMedium)
                frequencyOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = backupFrequency == option,
                            onClick = { backupFrequency = option; AppPreferences.setBackupFrequency(context, option) })
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            val lastBackup = AppPreferences.getLastBackupTime(context)
            if (lastBackup.isNotEmpty())
                Text("Last backup: $lastBackup", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

            backupViewModel.backupStatus.value.let { status ->
                if (status.isNotEmpty())
                    Text(status,
                        color = if (status.startsWith("Error"))
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
            }

            Button(onClick = { backupViewModel.backupNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !backupViewModel.isBackingUp.value) {
                if (backupViewModel.isBackingUp.value)
                    CircularProgressIndicator(strokeWidth = 2.dp)
                else {
                    Icon(Icons.Filled.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup now")
                }
            }

            HorizontalDivider()

            SectionHeader(Icons.Filled.CloudDownload, "Restore")

            Text("Restore will overwrite your current local data with the latest backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(onClick = { backupViewModel.restoreNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !backupViewModel.isBackingUp.value) {
                Text("Restore from last backup")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}