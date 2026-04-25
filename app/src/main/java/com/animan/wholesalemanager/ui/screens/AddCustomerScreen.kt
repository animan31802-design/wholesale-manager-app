package com.animan.wholesalemanager.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    navController: NavController,
    customerId: String? = null
) {
    val viewModel: CustomerViewModel = viewModel()
    val context  = LocalContext.current
    val S        = AppLanguage.strings
    val scope    = rememberCoroutineScope()

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var name      by remember { mutableStateOf("") }
    var phone     by remember { mutableStateOf("") }
    var address   by remember { mutableStateOf("") }
    var latitude  by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var loaded    by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }

    // ── Geocoder helper ───────────────────────────────────────────────
    // Resolves lat/lng → address string and fills the address field.
    fun resolveAddress(lat: Double, lng: Double) {
        isGeocoding = true
        scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // API 33+: callback-based (must be called on main thread, but result
                        // comes asynchronously — we wrap it in a suspendCoroutine)
                        var result: String? = null
                        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                                result = addresses.firstOrNull()?.let { addr ->
                                    listOfNotNull(
                                        addr.subThoroughfare,
                                        addr.thoroughfare,
                                        addr.locality,
                                        addr.subAdminArea,
                                        addr.adminArea,
                                        addr.postalCode
                                    ).joinToString(", ")
                                }
                                cont.resume(result) {}
                            }
                        }
                        result
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        addresses?.firstOrNull()?.let { addr ->
                            listOfNotNull(
                                addr.subThoroughfare,   // house number
                                addr.thoroughfare,      // street
                                addr.locality,          // city
                                addr.subAdminArea,      // district
                                addr.adminArea,         // state
                                addr.postalCode         // PIN
                            ).joinToString(", ")
                        }
                    }
                } catch (_: Exception) { null }
            }
            isGeocoding = false
            if (!resolved.isNullOrBlank()) address = resolved
        }
    }

    // ── Receive location back from map picker screen ───────────────────
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLat = savedStateHandle?.get<Double>("lat")
    val pickedLng = savedStateHandle?.get<Double>("lng")

    LaunchedEffect(pickedLat, pickedLng) {
        if (pickedLat != null && pickedLng != null) {
            latitude  = pickedLat
            longitude = pickedLng
            resolveAddress(pickedLat, pickedLng)   // ← auto-fill address
            savedStateHandle.remove<Double>("lat")
            savedStateHandle.remove<Double>("lng")
        }
    }

    // ── Location permission + current location ─────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        latitude  = it.latitude
                        longitude = it.longitude
                        resolveAddress(it.latitude, it.longitude)   // ← auto-fill address
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    LaunchedEffect(Unit) { viewModel.fetchCustomers() }

    LaunchedEffect(viewModel.customerList.value) {
        if (!loaded && customerId != null) {
            viewModel.customerList.value.find { it.id == customerId }?.let {
                name      = it.name
                phone     = it.phone
                address   = it.address
                latitude  = it.latitude
                longitude = it.longitude
                loaded    = true
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
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(S.customerName) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text(S.phone) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            // Address field — auto-filled from location, still manually editable
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(S.address) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2, maxLines = 3,
                trailingIcon = {
                    if (isGeocoding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )

            // Location buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                    loc?.let {
                                        latitude  = it.latitude
                                        longitude = it.longitude
                                        resolveAddress(it.latitude, it.longitude)  // ← auto-fill
                                    }
                                }
                            } catch (_: SecurityException) {}
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
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

            // Coordinates card
            if (latitude != null && longitude != null) {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Lat: ${"%.5f".format(latitude)}, Lng: ${"%.5f".format(longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
                                name      = name.trim(),
                                phone     = phone.trim(),
                                address   = address.trim(),
                                latitude  = latitude,
                                longitude = longitude
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