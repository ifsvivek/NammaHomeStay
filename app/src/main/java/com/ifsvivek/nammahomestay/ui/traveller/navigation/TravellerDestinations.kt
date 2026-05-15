package com.ifsvivek.nammahomestay.ui.traveller.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.ifsvivek.nammahomestay.R

/** Bottom-bar tabs in the traveller shell. Labels resolve via [R.string]. */
enum class TravellerDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    BROWSE("browse", R.string.nav_find, Icons.Filled.Search),
    MY_INTERESTS("my_interests", R.string.nav_sent, Icons.Filled.MarkEmailRead),
    GUIDE("guide", R.string.nav_help, Icons.AutoMirrored.Filled.MenuBook),
}
