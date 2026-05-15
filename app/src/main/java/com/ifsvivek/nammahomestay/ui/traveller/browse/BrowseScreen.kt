package com.ifsvivek.nammahomestay.ui.traveller.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.ui.components.EmptyState
import com.ifsvivek.nammahomestay.ui.components.MapMarker
import com.ifsvivek.nammahomestay.ui.components.NammaTopBar
import com.ifsvivek.nammahomestay.ui.components.OsmMap
import com.ifsvivek.nammahomestay.ui.components.PhotoImage
import com.ifsvivek.nammahomestay.ui.components.StatusPill
import org.osmdroid.util.GeoPoint

/** Traveller-side browse list of every LIVE homestay. Tap a card to open the detail screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onOpenHomestay: (hostId: String) -> Unit,
    trailingTopBarAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mapView by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NammaTopBar(
                title = "Find a homestay",
                trailing = if (trailingTopBarAction != null) {
                    { trailingTopBarAction() }
                } else null,
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search by name or village") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Live count + view toggle + sort dropdown.
            if (!state.loading && state.homestays.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.liveCount} live home${if (state.liveCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    ViewToggleButton(mapView = mapView, onToggle = { mapView = !mapView })
                    Spacer(Modifier.size(8.dp))
                    SortMenu(current = state.sort, onPick = viewModel::onSortChange)
                }
            }

            when {
                state.loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.homestays.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.Cottage,
                        title = "No live homestays yet",
                        subtitle = "Once a host turns on their three promises and adds a photo, their home will show up here.",
                    )
                }

                state.filtered.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "Nothing matched “${state.query}”",
                        subtitle = "Try a shorter word, or clear the search to see every home.",
                    )
                }

                mapView -> {
                    val pinned = state.filtered.filter { it.hasMapPin }
                    val unpinnedCount = state.filtered.size - pinned.size

                    if (pinned.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            EmptyState(
                                icon = Icons.Filled.Map,
                                title = "No homes pinned on the map yet",
                                subtitle = if (unpinnedCount > 0) {
                                    "$unpinnedCount home${if (unpinnedCount == 1) " hasn't" else "s haven't"} pinned a location. Switch back to the List view to see them."
                                } else {
                                    "Live homes will appear here once their hosts pin a location."
                                },
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (unpinnedCount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text(
                                        "${pinned.size} of ${state.filtered.size} pinned · others are in List view",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            ) {
                                OsmMap(
                                    modifier = Modifier.fillMaxSize(),
                                    center = GeoPoint(pinned.first().latitude!!, pinned.first().longitude!!),
                                    zoom = 12.0,
                                    markers = pinned.map {
                                        MapMarker(
                                            id = it.id,
                                            lat = it.latitude!!,
                                            lng = it.longitude!!,
                                            title = it.name.ifBlank { "A homestay" },
                                            snippet = it.location.ifBlank { null },
                                        )
                                    },
                                    onMarkerClick = { onOpenHomestay(it.id) },
                                )
                            }
                        }
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.filtered, key = { it.id }) { home ->
                        HomestayCard(
                            home = home,
                            todaysMenu = state.menusByHostId[home.id],
                            rating = state.ratingsByHostId[home.id],
                            onClick = { onOpenHomestay(home.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomestayCard(
    home: Homestay,
    todaysMenu: com.ifsvivek.nammahomestay.data.model.DailyMenu?,
    rating: com.ifsvivek.nammahomestay.data.model.AggregateRating?,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
    ) {
        Column {
            PhotoImage(
                blob = home.images.firstOrNull(),
                contentDescription = "${home.name} — first photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        home.name.ifBlank { "A homestay" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        "● LIVE",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                if (home.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            home.location,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (rating != null) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                "%.1f".format(rating.averageStars) + " (${rating.count})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (todaysMenu != null && !todaysMenu.isEmpty) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "Today: ${todaysMenu.dishName.ifBlank { "see menu" }} · ₹${todaysMenu.price}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(current: BrowseSort, onPick: (BrowseSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = { open = true },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    current.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            BrowseSort.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    onClick = {
                        onPick(opt)
                        open = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewToggleButton(mapView: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (mapView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        onClick = onToggle,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                if (mapView) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                contentDescription = null,
                tint = if (mapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                if (mapView) "List" else "Map",
                style = MaterialTheme.typography.labelLarge,
                color = if (mapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
