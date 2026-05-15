package com.ifsvivek.nammahomestay.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ifsvivek.nammahomestay.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

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
 * - When [autoFitToMarkers] is true (default) and there are 1+ markers, the
 *   camera zooms to the bounding box of every pin on every change, so the user
 *   doesn't have to pan around looking for them.
 * - Always shows OpenStreetMap's "© contributors" credit (TOS requirement).
 *
 * @param center initial centre of the map when there are no markers / no
 *   `recenterOn` request.
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
    autoFitToMarkers: Boolean = true,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Rasterise the vector pin to a Bitmap *once* — osmdroid's Marker.icon
    // sometimes refuses to honour a raw VectorDrawable, leaving an invisible pin.
    val pinDrawable: Drawable = remember(context) {
        rasterizeDrawable(context, R.drawable.ic_map_pin)
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK) // standard OSM
            setMultiTouchControls(true)
            // OSM TOS: keep the © overlay visible.
            overlays.add(CopyrightOverlay(context))
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

    // Rebuild markers + tap overlay whenever the inputs change. We keep the
    // CopyrightOverlay around (index 0) and only touch markers / event overlay.
    DisposableEffect(markers, onMarkerClick, onMapTap) {
        // Remove everything except the © overlay (always index 0).
        mapView.overlays.subList(1, mapView.overlays.size).clear()

        if (onMapTap != null) {
            mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) onMapTap(p)
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))
        }

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

        if (autoFitToMarkers && markers.isNotEmpty()) {
            // Zoom to fit every pin, with sensible padding. Post so the map has
            // a measured size first (otherwise zoomToBoundingBox is a no-op).
            mapView.post {
                if (markers.size == 1) {
                    val only = markers.first()
                    mapView.controller.animateTo(GeoPoint(only.lat, only.lng))
                    mapView.controller.setZoom(14.0)
                } else {
                    val points = markers.map { GeoPoint(it.lat, it.lng) }
                    val box = BoundingBox.fromGeoPointsSafe(points)
                    mapView.zoomToBoundingBox(box, true, MARGIN_PX)
                }
            }
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

/** Padding in pixels for [MapView.zoomToBoundingBox] — keeps pins off the edges. */
private const val MARGIN_PX = 96

/**
 * Paints a (potentially vector) drawable into a fresh ARGB bitmap and wraps it
 * in a [BitmapDrawable] — the form osmdroid's [Marker.icon] reliably renders.
 */
private fun rasterizeDrawable(context: Context, @DrawableRes id: Int): Drawable {
    val src = ContextCompat.getDrawable(context, id)
        ?: return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)!!
    val w = src.intrinsicWidth.coerceAtLeast(48)
    val h = src.intrinsicHeight.coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    src.setBounds(0, 0, w, h)
    src.draw(Canvas(bmp))
    return BitmapDrawable(context.resources, bmp)
}
