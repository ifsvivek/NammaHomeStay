package com.ifsvivek.nammahomestay.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.INDIA_CENTRE
import com.ifsvivek.nammahomestay.ui.components.MapMarker
import com.ifsvivek.nammahomestay.ui.components.OsmMap
import org.osmdroid.util.GeoPoint

/**
 * Full-screen map picker. The host taps anywhere on the map to drop a pin,
 * optionally hits "Use my location" to snap it to their current spot, then taps
 * the big "Save" button. Open from [HomeProfileScreen] via the
 * `pin_location` nav route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapPinViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var recenterRequest by remember { mutableStateOf<GeoPoint?>(null) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) fetchLastLocation(context) { lat, lng ->
            viewModel.onMapTap(lat, lng)
            recenterRequest = GeoPoint(lat, lng)
        }
    }

    LaunchedEffect(state.justSaved) {
        if (state.justSaved) {
            snackbar.showSnackbar("Pin saved ✓")
            viewModel.consumeSaved()
            onBack()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Pin your home", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(inner)) {
            Text(
                "Tap on the map to drop a pin where your home is. Travellers will see it on their map.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // The map itself fills the remaining space above the bottom bar.
            val pin = if (state.draftIsSet) {
                MapMarker(
                    id = "pin",
                    lat = state.draftLat!!,
                    lng = state.draftLng!!,
                    title = "Your home",
                )
            } else null

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                OsmMap(
                    modifier = Modifier.fillMaxSize(),
                    center = pin?.let { GeoPoint(it.lat, it.lng) } ?: INDIA_CENTRE,
                    zoom = if (pin != null) 14.0 else 4.5,
                    markers = listOfNotNull(pin),
                    recenterOn = recenterRequest,
                    onMapTap = { p -> viewModel.onMapTap(p.latitude, p.longitude) },
                )
            }

            // Bottom action bar: "Use my location" / "Save" / "Remove pin".
            Column(Modifier.padding(16.dp)) {
                Row {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(48.dp),
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                fetchLastLocation(context) { lat, lng ->
                                    viewModel.onMapTap(lat, lng)
                                    recenterRequest = GeoPoint(lat, lng)
                                }
                            } else {
                                locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Use my location")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    BigActionButton(
                        text = "Save pin",
                        icon = Icons.Filled.Save,
                        enabled = state.canSave,
                        onClick = viewModel::save,
                    )
                    if (state.saving) CircularProgressIndicator(strokeWidth = 3.dp)
                }
                if (state.savedLat != null) {
                    TextButton(
                        onClick = viewModel::clearPin,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Remove pin")
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLastLocation(
    context: android.content.Context,
    onLocation: (Double, Double) -> Unit,
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    // We've checked the permission at the call site.
    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { loc -> if (loc != null) onLocation(loc.latitude, loc.longitude) }
}
