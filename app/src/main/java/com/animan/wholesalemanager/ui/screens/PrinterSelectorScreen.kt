package com.animan.wholesalemanager.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.PrinterPreferences

data class BtDevice(val name: String, val address: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSelectorScreen(navController: NavController) {
    val context = LocalContext.current

    var selectedAddress by remember {
        mutableStateOf(PrinterPreferences.getSavedAddress(context))
    }
    var pairedDevices by remember { mutableStateOf<List<BtDevice>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf("") }

    fun loadDevices() {
        statusMessage = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                statusMessage = "Bluetooth permission not granted. Please allow it in App Settings."
                return
            }
        }

        // Use BluetoothManager (recommended) with fallback for older APIs
        val adapter: BluetoothAdapter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        if (adapter == null) {
            statusMessage = "Bluetooth is not available on this device."
            return
        }

        if (!adapter.isEnabled) {
            statusMessage = "Bluetooth is turned off. Please enable Bluetooth and try again."
            return
        }

        val bonded = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            statusMessage = "Permission denied reading paired devices. Check app permissions."
            return
        }

        if (bonded.isNullOrEmpty()) {
            statusMessage = "No paired devices found. Pair your printer in Android Settings → Bluetooth, then come back and tap Refresh."
            pairedDevices = emptyList()
            return
        }

        pairedDevices = bonded.mapNotNull { device ->
            try {
                BtDevice(
                    name = device.name ?: "Unknown device",
                    address = device.address
                )
            } catch (e: SecurityException) {
                null
            }
        }

        if (pairedDevices.isEmpty()) {
            statusMessage = "Could not read device names. Check Bluetooth permissions in App Settings."
        }
    }

    // KEY FIX: load inside LaunchedEffect, not inside remember {}
    // remember runs synchronously during composition and can miss permissions
    // that were granted just before opening this screen.
    LaunchedEffect(Unit) {
        loadDevices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select printer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadDevices() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            if (statusMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { loadDevices() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
                return@Column
            }

            Text("Select your thermal printer:")

            if (savedMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(savedMessage, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(pairedDevices) { device ->
                    val isSelected = device.address == selectedAddress
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { selectedAddress = device.address; savedMessage = "" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null)
                                Column {
                                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedAddress?.let { addr ->
                        PrinterPreferences.saveAddress(context, addr)
                        val name = pairedDevices.find { it.address == addr }?.name ?: addr
                        savedMessage = "Saved: $name"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAddress != null
            ) {
                Text("Save printer selection")
            }

            if (selectedAddress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        PrinterPreferences.clearSavedAddress(context)
                        selectedAddress = null
                        savedMessage = "Printer selection cleared."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear selection")
                }
            }
        }
    }
}