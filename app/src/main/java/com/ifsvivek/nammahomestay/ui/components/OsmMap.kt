package com.ifsvivek.nammahomestay.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ifsvivek.nammahomestay.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay

/** One pin on the [OsmMap]. */
data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String? = null,
    val snippet: String? = null,
)

/**
 * Thin Compose wrapper around an [osmdroid][org.osmdroid] MapView.
 *
 * - Renders OpenStreetMap tiles (no Google Maps API key needed).
 * - Forwards Android lifecycle events into the map (required by osmdroid).
 * - Lets the caller put down a list of [markers], react to taps on them via
 *   [onMarkerClick], or capture a single map-tap point via [onMapTap]
 *   (handy for the host's pin-picker flow).
 *
 * @param center initial centre of the map (sensible default: middle of India).
 * @param zoom initial zoom level (4 ≈ subcontinent, 14 ≈ village).
 * @param recenterOn if non-null, re-centres the map on this point whenever it
 *   changes — used by the host picker to follow the pin after "Use my location".
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = INDIA_CENTRE,
    zoom: Double = 4.5,
    markers: List<MapMarker> = emptyList(),
    onMarkerClick: ((MapMarker) -> Unit)? = null,
    onMapTap: ((GeoPoint) -> Unit)? = null,
    recenterOn: GeoPoint? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK) // standard OSM
            setMultiTouchControls(true)
            controller.setZoom(zoom)
            controller.setCenter(center)
        }
    }

    // Wire osmdroid's lifecycle expectations to the host Compose lifecycle.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            mapView.onDetach()
        }
    }

    // Rebuild markers + tap overlay whenever the inputs change.
    DisposableEffect(markers, onMarkerClick, onMapTap) {
        mapView.overlays.clear()

        if (onMapTap != null) {
            mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) onMapTap(p)
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))
        }

        val pinDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
        markers.forEach { m ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(m.lat, m.lng)
                title = m.title
                snippet = m.snippet
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = pinDrawable
                if (onMarkerClick != null) {
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(m)
                        true
                    }
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
        onDispose { /* overlays cleared on next composition or detach */ }
    }

    DisposableEffect(recenterOn) {
        if (recenterOn != null) {
            mapView.controller.animateTo(recenterOn)
            mapView.controller.setZoom(maxOf(mapView.zoomLevelDouble, 13.0))
        }
        onDispose { }
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
    }
}

/** Centre of India — a reasonable default when there are no pins to focus on. */
val INDIA_CENTRE: GeoPoint = GeoPoint(20.5937, 78.9629)
