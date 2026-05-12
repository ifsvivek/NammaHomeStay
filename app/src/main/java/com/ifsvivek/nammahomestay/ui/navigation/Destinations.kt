package com.ifsvivek.nammahomestay.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

/** The four tabs in the bottom bar. Labels are deliberately plain, no jargon. */
enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "My Home", Icons.Filled.Cottage),
    MENU("menu", "Today's Menu", Icons.Filled.Restaurant),
    INQUIRIES("inquiries", "Interests", Icons.Filled.MarkEmailRead),
    GUIDE("guide", "Help", Icons.AutoMirrored.Filled.MenuBook),
}
