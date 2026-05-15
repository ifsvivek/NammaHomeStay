package com.ifsvivek.nammahomestay.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.ifsvivek.nammahomestay.R

/**
 * The four tabs in the host bottom bar. Labels are localized via `nav_*` keys —
 * see `res/values/strings.xml` and the `values-hi` / `values-kn` overlays.
 */
enum class TopDestination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Filled.Cottage),
    MENU("menu", R.string.nav_menu, Icons.Filled.Restaurant),
    INQUIRIES("inquiries", R.string.nav_interests, Icons.Filled.MarkEmailRead),
    GUIDE("guide", R.string.nav_help, Icons.AutoMirrored.Filled.MenuBook),
}
