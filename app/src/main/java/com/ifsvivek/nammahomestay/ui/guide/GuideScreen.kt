package com.ifsvivek.nammahomestay.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.ui.components.NammaTopBar
import com.ifsvivek.nammahomestay.ui.components.SectionCard
import com.ifsvivek.nammahomestay.ui.theme.CallGreen
import com.ifsvivek.nammahomestay.util.dialPhoneNumber

/** Replace with your real support line before shipping. */
private const val SUPPORT_PHONE = "+911800000000"

private data class GuideItem(val icon: ImageVector, val title: String, val body: String)

private val guideItems = listOf(
    GuideItem(
        Icons.Filled.Restaurant,
        "Post today's menu",
        "Open the “Today's Menu” button, add one photo of the food, type the dish name and the price, then tap Post. That's it — guests see it right away.",
    ),
    GuideItem(
        Icons.Filled.PhotoCamera,
        "Add photos of your home",
        "Go to “My Home”, tap “Add photo”, and pick a clear picture of your room, kitchen or view. More photos means more guests.",
    ),
    GuideItem(
        Icons.Filled.CheckCircle,
        "Turn ON your three promises",
        "Clean bedding, a working washroom, and drinking water. When all three are ON and you have a photo, your home goes LIVE for travellers.",
    ),
    GuideItem(
        Icons.Filled.Visibility,
        "What “LIVE” means",
        "LIVE means travellers can find your home in the app. NOT LIVE means only you can see it. Finish the setup steps to go LIVE.",
    ),
    GuideItem(
        Icons.Filled.Call,
        "How guests reach you",
        "When a traveller is interested, you'll see them under “Incoming Interests”. Tap their name, then tap the green “Call Guest” button to talk to them directly.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = { NammaTopBar("Guide & Help") },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(guideItems) { item ->
                SectionCard(title = item.title, icon = item.icon) {
                    Text(
                        item.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            item {
                SectionCard(title = "Need help?", icon = Icons.Filled.Call) {
                    Text(
                        "Call our team and we will help you set up your home.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    BigActionButton(
                        text = "Call support",
                        icon = Icons.Filled.Call,
                        onClick = { context.dialPhoneNumber(SUPPORT_PHONE) },
                        containerColor = CallGreen,
                        contentColor = Color.White,
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text("  Sign out", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
