package com.ifsvivek.nammahomestay.ui.traveller.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Bottom-bar tabs in the traveller shell. */
enum class TravellerDestination(val route: String, val label: String, val icon: ImageVector) {
    BROWSE("browse", "Find", Icons.Filled.Search),
    MY_INTERESTS("my_interests", "Sent", Icons.Filled.MarkEmailRead),
    GUIDE("guide", "Help", Icons.AutoMirrored.Filled.MenuBook),
}
