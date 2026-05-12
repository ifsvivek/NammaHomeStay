package com.ifsvivek.nammahomestay.ui.menu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.Blob
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.PhotoImage
import com.ifsvivek.nammahomestay.ui.components.SuccessCheck

/**
 * The "60-Second Menu" — built to feel like posting a WhatsApp status:
 * one photo, one name, one price, one big button. Publishing is a single
 * Firestore `set()` (see [MenuViewModel.publish] / [com.ifsvivek.nammahomestay.data.repository.MenuRepository.save]).
 */
@Composable
fun DailyMenuScreen(
    modifier: Modifier = Modifier,
    onMenuPublished: () -> Unit = {},
    viewModel: MenuViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> viewModel.onImagePicked(uri) }

    // Fire the "internet did its job" feedback up to the host screen (snackbar).
    LaunchedEffect(state.justPublished) {
        if (state.justPublished) {
            onMenuPublished()
            viewModel.consumePublished()
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
        ) {
            Text(
                "Today's Menu",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Show guests what's cooking today. Takes under a minute.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            // ── 1. The photo ────────────────────────────────────────────────
            MenuPhotoSlot(
                pickedUri = state.pickedImage,
                storedPhoto = state.published?.image,
                onTap = { pickImage.launch("image/*") },
            )
            Spacer(Modifier.height(20.dp))

            // ── 2. Dish name ────────────────────────────────────────────────
            OutlinedTextField(
                value = state.dishName,
                onValueChange = viewModel::onDishNameChange,
                label = { Text("What is the dish?") },
                placeholder = { Text("e.g. Ragi mudde & saaru") },
                leadingIcon = { Icon(Icons.Filled.Restaurant, contentDescription = null) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // ── 3. Price ────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.priceText,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Price per plate") },
                placeholder = { Text("e.g. 120") },
                leadingIcon = { Icon(Icons.Filled.CurrencyRupee, contentDescription = null) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))

            // ── 4. Publish ──────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                BigActionButton(
                    text = if (state.hasPublishedMenu) "Update today's menu" else "Post today's menu",
                    icon = Icons.Filled.Restaurant,
                    enabled = state.canPublish,
                    onClick = { viewModel.publish(context) },
                )
                if (state.saving) CircularProgressIndicator(strokeWidth = 3.dp)
            }

            if (state.hasPublishedMenu) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = viewModel::clearMenu,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Take today's menu down", style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    state.error!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Big celebratory tick over everything for a moment after publish.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SuccessCheck(visible = state.justPublished)
        }
    }
}

@Composable
private fun MenuPhotoSlot(
    pickedUri: android.net.Uri?,
    storedPhoto: Blob?,
    onTap: () -> Unit,
) {
    val hasPhoto = pickedUri != null || storedPhoto != null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        when {
            pickedUri != null -> AsyncImage(
                model = pickedUri,
                contentDescription = "Today's dish photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            storedPhoto != null -> PhotoImage(
                blob = storedPhoto,
                contentDescription = "Today's dish photo",
                modifier = Modifier.fillMaxSize(),
            )

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    "Tap to add a photo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // A small "change photo" affordance once something is shown.
        if (hasPhoto) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onTap)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
