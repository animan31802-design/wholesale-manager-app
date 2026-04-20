package com.animan.wholesalemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    // Load saved preferences
    var shopName         by remember { mutableStateOf(AppPreferences.getShopName(context)) }
    var backupEnabled    by remember { mutableStateOf(AppPreferences.isBackupEnabled(context)) }
    var backupFrequency  by remember { mutableStateOf(AppPreferences.getBackupFrequency(context)) }
    var savedMessage     by remember { mutableStateOf("") }
    var backupStatus     by remember { mutableStateOf("") }
    var isBackingUp      by remember { mutableStateOf(false) }

    val frequencyOptions = listOf("Manual only", "Daily", "Weekly")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Shop info ─────────────────────────────────────────────
            SettingsSectionHeader(icon = { Icon(Icons.Filled.Store, null) }, title = "Shop details")

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it; savedMessage = "" },
                label = { Text("Shop name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (savedMessage.isNotEmpty()) {
                Text(savedMessage, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    AppPreferences.setShopName(context, shopName.trim())
                    savedMessage = "Shop name saved."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save shop name") }

            HorizontalDivider()

            // ── Printer ───────────────────────────────────────────────
            SettingsSectionHeader(icon = { Icon(Icons.Filled.Print, null) }, title = "Printer")

            OutlinedButton(
                onClick = { navController.navigate("printer_selector") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Select / change Bluetooth printer") }

            HorizontalDivider()

            // ── Firebase backup ───────────────────────────────────────
            SettingsSectionHeader(icon = { Icon(Icons.Filled.CloudUpload, null) }, title = "Cloud backup")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enable automatic backup")
                    Text(
                        "Backs up your data to Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = backupEnabled,
                    onCheckedChange = {
                        backupEnabled = it
                        AppPreferences.setBackupEnabled(context, it)
                    }
                )
            }

            if (backupEnabled) {
                Text("Backup frequency", style = MaterialTheme.typography.labelMedium)
                frequencyOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = backupFrequency == option,
                            onClick = {
                                backupFrequency = option
                                AppPreferences.setBackupFrequency(context, option)
                            }
                        )
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            if (backupStatus.isNotEmpty()) {
                Text(backupStatus, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    isBackingUp = true
                    backupStatus = "Backing up…"
                    // Firebase backup implementation will go here in Phase 4
                    // For now we show a placeholder message
                    isBackingUp = false
                    backupStatus = "Backup complete. (Firebase sync coming in Phase 4)"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBackingUp
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup now")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}