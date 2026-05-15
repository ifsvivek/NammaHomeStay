package com.ifsvivek.nammahomestay.ui.traveller

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ifsvivek.nammahomestay.ui.components.ModeSwitchButton
import com.ifsvivek.nammahomestay.ui.guide.GuideScreen
import com.ifsvivek.nammahomestay.ui.traveller.browse.BrowseScreen
import com.ifsvivek.nammahomestay.ui.traveller.detail.HomestayDetailScreen
import com.ifsvivek.nammahomestay.ui.traveller.myinterests.MyInterestsScreen
import com.ifsvivek.nammahomestay.ui.traveller.navigation.TravellerDestination

/**
 * The traveller-side shell: bottom navigation across Find / Sent / Help, plus
 * a non-nav "homestay detail" route pushed from Find and Sent.
 */
@Composable
fun TravellerMainScreen(
    onSwitchToHost: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun goTo(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // The mode-switch pill is shown on the *top* tabs. Hide it on the detail
    // screen so the back arrow keeps its prominence.
    val showSwitch = currentRoute in TravellerDestination.entries.map { it.route }
    val modeSwitchAction: (@Composable () -> Unit)? = if (showSwitch) {
        { ModeSwitchButton(label = "Host mode", onClick = onSwitchToHost) }
    } else null

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showSwitch) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        TravellerDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = { goTo(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = {
                                    Text(
                                        dest.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = TravellerDestination.BROWSE.route,
            modifier = Modifier.padding(inner),
        ) {
            composable(TravellerDestination.BROWSE.route) {
                BrowseScreen(
                    onOpenHomestay = { hostId -> nav.navigate("detail/$hostId") },
                    trailingTopBarAction = modeSwitchAction,
                )
            }
            composable(TravellerDestination.MY_INTERESTS.route) {
                MyInterestsScreen(
                    onOpenHomestay = { hostId -> nav.navigate("detail/$hostId") },
                    trailingTopBarAction = modeSwitchAction,
                )
            }
            composable(TravellerDestination.GUIDE.route) {
                GuideScreen(onSignOut = onSignOut)
            }
            composable(
                route = "detail/{hostId}",
                arguments = listOf(navArgument("hostId") { type = NavType.StringType }),
            ) {
                HomestayDetailScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
