package com.animan.wholesalemanager.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint               // FIX: osmdroid GeoPoint, NOT Firebase
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedLat by remember { mutableStateOf<Double?>(null) }
    var selectedLng by remember { mutableStateOf<Double?>(null) }
    var mapViewRef  by remember { mutableStateOf<MapView?>(null) }

    // FIX 2: Configure osmdroid before creating MapView
    DisposableEffect(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", 0))
            userAgentValue = context.packageName
        }
        onDispose {}
    }

    // FIX 3: Pause/resume map with lifecycle to prevent leaks
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME  -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick location") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Map fills the whole screen
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        // Default center: Chennai
                        controller.setCenter(GeoPoint(13.0827, 80.2707))

                        // FIX 1: Correct osmdroid overlay, correct GeoPoint
                        overlays.add(object : Overlay() {
                            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                                // FIX 1: cast to osmdroid GeoPoint (not Firebase)
                                val gp = mapView.projection.fromPixels(
                                    e.x.toInt(), e.y.toInt()
                                ) as GeoPoint

                                selectedLat = gp.latitude
                                selectedLng = gp.longitude

                                // Remove old marker, add new one
                                overlays.removeAll { it is Marker }
                                val marker = Marker(mapView).apply {
                                    position = gp
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Selected location"
                                }
                                overlays.add(marker)
                                invalidate()
                                return true
                            }
                        })

                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Instruction text at top
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Text(
                    "Tap on the map to select location",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Confirm button at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                if (selectedLat != null) {
                    Text(
                        "Lat: ${"%.5f".format(selectedLat)}, Lng: ${"%.5f".format(selectedLng)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        if (selectedLat != null && selectedLng != null) {
                            navController.previousBackStackEntry?.savedStateHandle?.apply {
                                set("lat", selectedLat)
                                set("lng", selectedLng)
                            }
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedLat != null
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm location")
                }
            }
        }
    }
}