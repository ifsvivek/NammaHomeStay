package com.ifsvivek.nammahomestay.ui.inquiry

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.data.model.Inquiry
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.EmptyState
import com.ifsvivek.nammahomestay.ui.components.NammaTopBar
import com.ifsvivek.nammahomestay.ui.components.StatusPill
import com.ifsvivek.nammahomestay.ui.theme.CallGreen
import com.ifsvivek.nammahomestay.util.dialPhoneNumber
import java.util.Date

/** The "Inquiry Box": a list of incoming interests, each one tap from a call-back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InquiryScreen(
    modifier: Modifier = Modifier,
    trailingTopBarAction: (@Composable () -> Unit)? = null,
    viewModel: InquiryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            NammaTopBar(
                title = "Incoming Interests",
                trailing = if (trailingTopBarAction != null) {
                    { trailingTopBarAction() }
                } else null,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.inquiries.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.MarkEmailRead,
                    title = "No interests yet",
                    subtitle = "When travellers want to stay or eat at your home, they will show up here.",
                    action = {
                        OutlinedButton(onClick = viewModel::addSampleInquiry) {
                            Text("Add a sample interest (for testing)")
                        }
                    },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.inquiries, key = { it.id }) { inquiry ->
                    InquiryCard(
                        inquiry = inquiry,
                        expanded = expandedId == inquiry.id,
                        onToggle = {
                            expandedId = if (expandedId == inquiry.id) null else inquiry.id
                        },
                        onCall = {
                            context.dialPhoneNumber(inquiry.guestPhone)
                            viewModel.markCalled(inquiry.id)
                        },
                        onClose = { viewModel.markClosed(inquiry.id) },
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = viewModel::addSampleInquiry,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Add a sample interest (for testing)") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InquiryCard(
    inquiry: Inquiry,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCall: () -> Unit,
    onClose: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = onToggle,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        inquiry.guestName.ifBlank { "A traveller" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        relativeTime(inquiry.timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when (inquiry.status) {
                    "pending" -> StatusPill(
                        "NEW",
                        container = MaterialTheme.colorScheme.tertiary,
                        content = MaterialTheme.colorScheme.onTertiary,
                    )
                    "called" -> StatusPill(
                        "CALLED",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    else -> StatusPill(
                        "CLOSED",
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Phone: ${inquiry.guestPhone}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    BigActionButton(
                        text = "Call Guest",
                        icon = Icons.Filled.Call,
                        onClick = onCall,
                        containerColor = CallGreen,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                        Text("Mark as done", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

private fun relativeTime(date: Date?): String {
    if (date == null) return "Just now"
    return DateUtils.getRelativeTimeSpanString(
        date.time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}
