package com.animan.wholesalemanager.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.animan.wholesalemanager.utils.AppLanguage
import com.animan.wholesalemanager.viewmodel.CustomerViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    navController: NavController,
    customerId: String? = null
) {
    val viewModel: CustomerViewModel = viewModel()
    val context  = LocalContext.current
    val S        = AppLanguage.strings

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var name      by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var address   by remember { mutableStateOf("") }
    var latitude  by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var loaded    by remember { mutableStateOf(false) }

    // Receive location back from picker screen
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLat = savedStateHandle?.get<Double>("lat")
    val pickedLng = savedStateHandle?.get<Double>("lng")

    LaunchedEffect(pickedLat, pickedLng) {
        if (pickedLat != null && pickedLng != null) {
            latitude = pickedLat; longitude = pickedLng
            // Clear so re-entering screen doesn't re-apply old value
            savedStateHandle.remove<Double>("lat")
            savedStateHandle.remove<Double>("lng")
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    loc?.let { latitude = it.latitude; longitude = it.longitude }
                }
            } catch (_: SecurityException) {}
        }
    }

    LaunchedEffect(Unit) { viewModel.fetchCustomers() }

    LaunchedEffect(viewModel.customerList.value) {
        if (!loaded && customerId != null) {
            viewModel.customerList.value.find { it.id == customerId }?.let {
                name = it.name; phone = it.phone; address = it.address
                latitude = it.latitude; longitude = it.longitude; loaded = true
            }
        }
    }

    val isEditMode = customerId != null

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isEditMode) S.editCustomer else S.addCustomer) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text(S.customerName) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = phone, onValueChange = { phone = it },
                label = { Text(S.phone) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = address, onValueChange = { address = it },
                label = { Text(S.address) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)

            // Location buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let { latitude = it.latitude; longitude = it.longitude }
                            }
                        } catch (_: SecurityException) {}
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.MyLocation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S.useCurrentLocation, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { navController.navigate("location_picker") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S.pickOnMap, maxLines = 1)
                }
            }

            if (latitude != null && longitude != null) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lat: ${"%.5f".format(latitude)}, Lng: ${"%.5f".format(longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        // Open in maps
                        TextButton(onClick = {
                            val uri = Uri.parse("geo:${latitude},${longitude}?q=${latitude},${longitude}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }) { Text("View", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            viewModel.errorMessage.value?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (isEditMode) {
                        val existing = viewModel.customerList.value.find { it.id == customerId }
                        existing?.let {
                            viewModel.updateCustomer(it.copy(
                                name = name.trim(), phone = phone.trim(),
                                address = address.trim(), latitude = latitude, longitude = longitude
                            )) { navController.popBackStack() }
                        }
                    } else {
                        viewModel.addCustomer(name, phone, address, latitude, longitude) {
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading.value
            ) {
                if (viewModel.isLoading.value) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (isEditMode) S.update else S.save)
            }
        }
    }
}