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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable screen for adding/editing both Customers and Suppliers.
 *
 * showLocation = true  → shows GPS + map picker + geocoding + coordinates card (for customers)
 * showLocation = false → hides all location UI (for suppliers)
 *
 * onSave gives back (name, phone, address, latitude?, longitude?)
 * For suppliers just ignore lat/lng in the lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    title        : String,
    navController: NavController,
    initialName  : String  = "",
    initialPhone : String  = "",
    initialAddr  : String  = "",
    initialLat   : Double? = null,
    initialLng   : Double? = null,
    showLocation : Boolean = true,
    isLoading    : Boolean = false,
    errorMessage : String? = null,
    onSave       : (name: String, phone: String, address: String,
                    latitude: Double?, longitude: Double?) -> Unit,
    onBack       : () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var name        by remember { mutableStateOf(initialName) }
    var phone       by remember { mutableStateOf(initialPhone) }
    var address     by remember { mutableStateOf(initialAddr) }
    var latitude    by remember { mutableStateOf(initialLat) }
    var longitude   by remember { mutableStateOf(initialLng) }
    var isGeocoding by remember { mutableStateOf(false) }

    // ── Geocoder — lat/lng → readable address string ──────────────────
    fun resolveAddress(lat: Double, lng: Double) {
        isGeocoding = true
        scope.launch {
            val resolved = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                                addr.subThoroughfare,
                                addr.thoroughfare,
                                addr.locality,
                                addr.subAdminArea,
                                addr.adminArea,
                                addr.postalCode
                            ).joinToString(", ")
                        }
                    }
                } catch (_: Exception) { null }
            }
            isGeocoding = false
            if (!resolved.isNullOrBlank()) address = resolved
        }
    }

    // ── Receive location picked from map picker screen ────────────────
    if (showLocation) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        val pickedLat = savedStateHandle?.get<Double>("lat")
        val pickedLng = savedStateHandle?.get<Double>("lng")

        LaunchedEffect(pickedLat, pickedLng) {
            if (pickedLat != null && pickedLng != null) {
                latitude  = pickedLat
                longitude = pickedLng
                resolveAddress(pickedLat, pickedLng)
                savedStateHandle.remove<Double>("lat")
                savedStateHandle.remove<Double>("lng")
            }
        }
    }

    // ── Location permission launcher ──────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        latitude  = it.latitude
                        longitude = it.longitude
                        resolveAddress(it.latitude, it.longitude)
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    // ── UI ────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Name
            OutlinedTextField(
                value           = name,
                onValueChange   = { name = it },
                label           = { Text("Name *") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )

            // Phone
            OutlinedTextField(
                value           = phone,
                onValueChange   = { phone = it },
                label           = { Text("Phone") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // Address — auto-filled from GPS when showLocation = true
            OutlinedTextField(
                value           = address,
                onValueChange   = { address = it },
                label           = { Text("Address") },
                modifier        = Modifier.fillMaxWidth(),
                minLines        = 2,
                maxLines        = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                trailingIcon = {
                    if (isGeocoding) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            )

            // Location buttons + coordinates card — only when showLocation = true
            if (showLocation) {

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Current location button
                    OutlinedButton(
                        onClick  = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                try {
                                    fusedLocationClient.lastLocation
                                        .addOnSuccessListener { loc: Location? ->
                                            loc?.let {
                                                latitude  = it.latitude
                                                longitude = it.longitude
                                                resolveAddress(it.latitude, it.longitude)
                                            }
                                        }
                                } catch (_: SecurityException) {}
                            } else {
                                locationPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.MyLocation, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Current location", maxLines = 1)
                    }

                    // Map picker button
                    OutlinedButton(
                        onClick  = { navController.navigate("location_picker") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.LocationOn, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick on map", maxLines = 1)
                    }
                }

                // Coordinates display card
                if (latitude != null && longitude != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Lat: ${"%.5f".format(latitude)}, " +
                                        "Lng: ${"%.5f".format(longitude)}",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    val uri = Uri.parse(
                                        "geo:$latitude,$longitude?q=$latitude,$longitude")
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri))
                                }
                            ) {
                                Text("View",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let {
                Text(it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // Save button
            Button(
                onClick  = {
                    onSave(
                        name.trim(),
                        phone.trim(),
                        address.trim(),
                        if (showLocation) latitude else null,
                        if (showLocation) longitude else null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled  = name.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Text("Save")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}