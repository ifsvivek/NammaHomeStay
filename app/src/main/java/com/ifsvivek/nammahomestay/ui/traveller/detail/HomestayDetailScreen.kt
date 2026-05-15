package com.ifsvivek.nammahomestay.ui.traveller.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wash
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.data.model.DailyMenu
import com.ifsvivek.nammahomestay.data.model.Homestay
import com.ifsvivek.nammahomestay.data.model.VerificationChecklist
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.PhotoImage
import com.ifsvivek.nammahomestay.ui.components.SectionCard
import com.ifsvivek.nammahomestay.ui.components.SuccessCheck
import com.ifsvivek.nammahomestay.ui.theme.CallGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomestayDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomestayDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.justSent) {
        if (state.justSent) {
            snackbar.showSnackbar("Sent! The host will see your interest.")
            viewModel.consumeSent()
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
                title = {
                    Text(
                        state.homestay?.name?.ifBlank { "Homestay" } ?: "Homestay",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
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
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.homestay == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Homestay not found.", style = MaterialTheme.typography.titleLarge)
                }
                else -> DetailContent(
                    home = state.homestay!!,
                    todaysMenu = state.todaysMenu,
                    sending = state.sending,
                    onSendInquiry = viewModel::sendInquiry,
                )
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SuccessCheck(visible = state.justSent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
    home: Homestay,
    todaysMenu: DailyMenu?,
    sending: Boolean,
    onSendInquiry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        // ── Photos ─────────────────────────────────────────────────────────
        item {
            if (home.images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No photos yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val pager = rememberPagerState(pageCount = { home.images.size })
                Box {
                    HorizontalPager(
                        state = pager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f),
                    ) { page ->
                        PhotoImage(
                            blob = home.images[page],
                            contentDescription = "Photo ${page + 1} of ${home.images.size}",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (home.images.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        ) {
                            Text(
                                "${pager.currentPage + 1} / ${home.images.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // ── Title block ────────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    home.name.ifBlank { "Homestay" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (home.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            home.location,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ── Promises chips ─────────────────────────────────────────────────
        item { PromisesRow(home.checklist) }

        // ── Today's menu ───────────────────────────────────────────────────
        if (todaysMenu != null && !todaysMenu.isEmpty) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    SectionCard(title = "Today's menu", icon = Icons.Filled.Restaurant) {
                        if (todaysMenu.image != null) {
                            PhotoImage(
                                blob = todaysMenu.image,
                                contentDescription = todaysMenu.dishName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Text(
                            todaysMenu.dishName.ifBlank { "Today's dish" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CurrencyRupee,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "${todaysMenu.price} per plate",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }

        // ── Big green "I'm interested" ─────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center) {
                    BigActionButton(
                        text = "I'm interested",
                        icon = Icons.Filled.Call,
                        onClick = onSendInquiry,
                        enabled = !sending,
                        containerColor = CallGreen,
                        contentColor = Color.White,
                    )
                    if (sending) CircularProgressIndicator(strokeWidth = 3.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "The host will see your name and phone, and call you back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromisesRow(checklist: VerificationChecklist) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        PromiseChip(Icons.Filled.Bed, "Clean bedding", checklist.cleanBedding)
        PromiseChip(Icons.Filled.Wash, "Washroom", checklist.functionalWashroom)
        PromiseChip(Icons.Filled.LocalDrink, "Drinking water", checklist.drinkingWater)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromiseChip(icon: ImageVector, label: String, on: Boolean) {
    AssistChip(
        onClick = {},
        enabled = false,
        leadingIcon = {
            Icon(
                if (on) Icons.Filled.CheckCircle else icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = if (on) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            disabledLabelColor = if (on) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledLeadingIconContentColor = if (on) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    )
}
