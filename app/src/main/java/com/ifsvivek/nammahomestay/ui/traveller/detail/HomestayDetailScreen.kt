package com.ifsvivek.nammahomestay.ui.traveller.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wash
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    LaunchedEffect(state.justReviewed) {
        if (state.justReviewed) {
            snackbar.showSnackbar("Thanks — your review is published.")
            viewModel.consumeReviewed()
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
                    reviews = state.reviews,
                    aggregate = state.aggregate,
                    canWriteReview = !state.travellerAlreadyReviewed,
                    submittingReview = state.submittingReview,
                    sending = state.sending,
                    onSendInquiry = viewModel::sendInquiry,
                    onSubmitReview = viewModel::submitReview,
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
    reviews: List<com.ifsvivek.nammahomestay.data.model.Review>,
    aggregate: com.ifsvivek.nammahomestay.data.model.AggregateRating?,
    canWriteReview: Boolean,
    submittingReview: Boolean,
    sending: Boolean,
    onSendInquiry: () -> Unit,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
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
                            modifier = Modifier.weight(1f),
                        )
                        if (aggregate != null) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(2.dp))
                            Text(
                                "%.1f".format(aggregate.averageStars) + " (${aggregate.count})",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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

        // ── Reviews ────────────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionCard(title = "Reviews", icon = Icons.Filled.Star) {
                    if (canWriteReview) {
                        WriteReviewForm(
                            submitting = submittingReview,
                            onSubmit = onSubmitReview,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    if (reviews.isEmpty()) {
                        Text(
                            if (canWriteReview) {
                                "Be the first to leave a review."
                            } else {
                                "Thanks for your review!"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        reviews.take(5).forEachIndexed { idx, r ->
                            if (idx > 0) Spacer(Modifier.height(12.dp))
                            ReviewRow(r)
                        }
                        if (reviews.size > 5) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "+ ${reviews.size - 5} more",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PromisesRow(checklist: VerificationChecklist) {
    // FlowRow so chips wrap to the next line on narrow screens instead of one
    // chip getting squeezed into a single-letter-per-line column.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        PromiseChip(Icons.Filled.Bed, "Clean bedding", checklist.cleanBedding)
        PromiseChip(Icons.Filled.Wash, "Washroom", checklist.functionalWashroom)
        PromiseChip(Icons.Filled.LocalDrink, "Drinking water", checklist.drinkingWater)
    }
}

@Composable
private fun WriteReviewForm(
    submitting: Boolean,
    onSubmit: (rating: Int, comment: String) -> Unit,
) {
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    Text(
        "How was your stay?",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { star ->
            IconButton(onClick = { rating = star }) {
                Icon(
                    if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$star star${if (star == 1) "" else "s"}",
                    tint = if (star <= rating) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
    OutlinedTextField(
        value = comment,
        onValueChange = { comment = it.take(500) },
        label = { Text("A line or two (optional)") },
        placeholder = { Text("e.g. The food was great and the host was so kind.") },
        minLines = 2,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Box(contentAlignment = Alignment.Center) {
        BigActionButton(
            text = if (submitting) "Sending…" else "Post review",
            icon = Icons.Filled.Star,
            enabled = rating > 0 && !submitting,
            onClick = { onSubmit(rating, comment) },
        )
        if (submitting) CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun ReviewRow(review: com.ifsvivek.nammahomestay.data.model.Review) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                review.travellerName.ifBlank { "A traveller" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            (1..5).forEach { star ->
                Icon(
                    if (star <= review.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (star <= review.rating) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (review.comment.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                review.comment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
