package com.ifsvivek.nammahomestay.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wash
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.SectionCard
import com.ifsvivek.nammahomestay.ui.components.SetupProgressCard
import com.ifsvivek.nammahomestay.ui.components.StatusPill

/** "My Home" — the host's digital shopfront, all in big cards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeProfileScreen(
    onOpenTodaysMenu: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> uri?.let { viewModel.addPhoto(context, it) } }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0), // outer (bottom-bar) Scaffold already inset us
        topBar = {
            TopAppBar(
                title = { Text("My Home", style = MaterialTheme.typography.headlineSmall) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenTodaysMenu,
                icon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                text = { Text("Today's Menu", style = MaterialTheme.typography.titleMedium) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { inner ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val home = state.homestay
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SetupProgressCard(
                    progress = state.progress,
                    doneCount = state.doneCount,
                    totalCount = state.totalCount,
                    isLive = state.isLive,
                )
            }

            // ── Status pill row ──────────────────────────────────────────
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isLive) {
                        StatusPill(
                            "● LIVE",
                            container = MaterialTheme.colorScheme.primary,
                            content = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        StatusPill(
                            "NOT LIVE YET",
                            container = MaterialTheme.colorScheme.tertiaryContainer,
                            content = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            // ── 1. Name & village ────────────────────────────────────────
            item { HomeBasicsCard(home, onSave = viewModel::saveBasics) }

            // ── 2. Photos ────────────────────────────────────────────────
            item {
                PhotosCard(
                    images = home?.images.orEmpty(),
                    uploading = state.savingPhoto,
                    onAdd = { pickPhoto.launch("image/*") },
                    onRemove = viewModel::removePhoto,
                )
            }

            // ── 3. Promises checklist ────────────────────────────────────
            item {
                ChecklistCard(
                    cleanBedding = home?.checklist?.cleanBedding == true,
                    functionalWashroom = home?.checklist?.functionalWashroom == true,
                    drinkingWater = home?.checklist?.drinkingWater == true,
                    onChange = viewModel::setChecklist,
                )
            }

            item { Spacer(Modifier.height(72.dp)) } // breathing room above the FAB
        }
    }
}

@Composable
private fun HomeBasicsCard(
    home: com.ifsvivek.nammahomestay.data.model.Homestay?,
    onSave: (name: String, location: String) -> Unit,
) {
    SectionCard(title = "Your home's name", icon = Icons.Filled.PhotoLibrary) {
        var name by rememberSaveable(home?.name) { mutableStateOf(home?.name.orEmpty()) }
        var place by rememberSaveable(home?.location) { mutableStateOf(home?.location.orEmpty()) }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(50) },
            label = { Text("Home name") },
            placeholder = { Text("e.g. Lakshmi Farm Stay") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = place,
            onValueChange = { place = it.take(60) },
            label = { Text("Village / area") },
            placeholder = { Text("e.g. Sakleshpur, Hassan") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        BigActionButton(
            text = "Save",
            enabled = name.isNotBlank(),
            onClick = { onSave(name, place) },
        )
    }
}

@Composable
private fun PhotosCard(
    images: List<String>,
    uploading: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    SectionCard(title = "Photos of your home", icon = Icons.Filled.PhotoLibrary) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !uploading, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uploading) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("Add photo", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            items(images, key = { it }) { url ->
                Box(modifier = Modifier.size(120.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Home photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { onRemove(url) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (images.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Add at least one photo so guests can see your home.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChecklistCard(
    cleanBedding: Boolean,
    functionalWashroom: Boolean,
    drinkingWater: Boolean,
    onChange: (cleanBedding: Boolean, functionalWashroom: Boolean, drinkingWater: Boolean) -> Unit,
) {
    SectionCard(title = "Promises to your guests", icon = Icons.Filled.Bed) {
        Text(
            "Your home goes LIVE only when all three are ON.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        ToggleRow(Icons.Filled.Bed, "Clean bedding", cleanBedding) {
            onChange(it, functionalWashroom, drinkingWater)
        }
        ToggleRow(Icons.Filled.Wash, "Functional washroom", functionalWashroom) {
            onChange(cleanBedding, it, drinkingWater)
        }
        ToggleRow(Icons.Filled.LocalDrink, "Drinking water", drinkingWater) {
            onChange(cleanBedding, functionalWashroom, it)
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
