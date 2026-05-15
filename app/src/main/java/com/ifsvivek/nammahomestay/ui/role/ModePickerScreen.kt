package com.ifsvivek.nammahomestay.ui.role

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.data.role.UserMode
import com.ifsvivek.nammahomestay.ui.components.BigChoiceCard

/**
 * One-time welcome shown right after the first OTP sign-in. The user picks a
 * name and then taps either "I'm hosting" or "I'm travelling" — that choice is
 * persisted in [UserModeStore] and can be flipped later from the mode-switch
 * icon in the top bar of either shell.
 */
@Composable
fun ModePickerScreen(
    modifier: Modifier = Modifier,
    roleViewModel: RoleViewModel = viewModel(),
) {
    var name by rememberSaveable { mutableStateOf(roleViewModel.currentDisplayName) }
    val nameValid = remember(name) { name.trim().isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Filled.Cottage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Welcome to Namma HomeStay",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("What should we call you?") },
            placeholder = { Text("e.g. Lakshmi or Ramesh") },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))

        Text(
            "What brings you here today?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BigChoiceCard(
                icon = Icons.Filled.Cottage,
                title = "I'm hosting",
                subtitle = "I have a homestay to offer",
                enabled = nameValid,
                onClick = { roleViewModel.choose(UserMode.HOST, name) },
            )
            BigChoiceCard(
                icon = Icons.Filled.Luggage,
                title = "I'm travelling",
                subtitle = "I'm looking for a homestay",
                enabled = nameValid,
                onClick = { roleViewModel.choose(UserMode.TRAVELLER, name) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "You can switch any time from the top of the screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
