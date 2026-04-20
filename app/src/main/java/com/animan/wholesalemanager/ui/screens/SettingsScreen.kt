package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val backupViewModel: BackupViewModel = viewModel()

    var shopName        by remember { mutableStateOf(AppPreferences.getShopName(context)) }
    var backupEnabled   by remember { mutableStateOf(AppPreferences.isBackupEnabled(context)) }
    var backupFrequency by remember { mutableStateOf(AppPreferences.getBackupFrequency(context)) }
    var shopNameSaved   by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Shop details ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Store, null)
                Text("Shop details", style = MaterialTheme.typography.titleMedium)
            }

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

            // ── Printer ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Print, null)
                Text("Printer", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { navController.navigate("printer_selector") },
                modifier = Modifier.fillMaxWidth()) {
                Text("Select / change Bluetooth printer")
            }

            HorizontalDivider()

            // ── Backup ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CloudUpload, null)
                Text("Cloud backup", style = MaterialTheme.typography.titleMedium)
            }

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Enable automatic backup")
                    Text("Backs up to Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = backupEnabled, onCheckedChange = {
                    backupEnabled = it; AppPreferences.setBackupEnabled(context, it)
                })
            }

            if (backupEnabled) {
                Text("Backup frequency", style = MaterialTheme.typography.labelMedium)
                frequencyOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = backupFrequency == option,
                            onClick = { backupFrequency = option; AppPreferences.setBackupFrequency(context, option) })
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Last backup time
            val lastBackup = AppPreferences.getLastBackupTime(context)
            if (lastBackup.isNotEmpty()) {
                Text("Last backup: $lastBackup", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            backupViewModel.backupStatus.value.let { status ->
                if (status.isNotEmpty())
                    Text(status, color = if (status.startsWith("Error"))
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { backupViewModel.backupNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !backupViewModel.isBackingUp.value
            ) {
                if (backupViewModel.isBackingUp.value) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Backup now")
                }
            }

            HorizontalDivider()

            // ── Restore ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CloudDownload, null)
                Text("Restore", style = MaterialTheme.typography.titleMedium)
            }
            Text("Restore will overwrite your current local data with the latest backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(
                onClick = { backupViewModel.restoreNow(context) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !backupViewModel.isBackingUp.value
            ) { Text("Restore from last backup") }

            Spacer(Modifier.height(16.dp))
        }
    }
}