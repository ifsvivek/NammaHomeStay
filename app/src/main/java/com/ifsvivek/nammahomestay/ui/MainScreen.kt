package com.ifsvivek.nammahomestay.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ifsvivek.nammahomestay.ui.components.ModeSwitchButton
import com.ifsvivek.nammahomestay.ui.guide.GuideScreen
import com.ifsvivek.nammahomestay.ui.home.HomeProfileScreen
import com.ifsvivek.nammahomestay.ui.inquiry.InquiryScreen
import com.ifsvivek.nammahomestay.ui.menu.DailyMenuScreen
import com.ifsvivek.nammahomestay.ui.navigation.TopDestination
import kotlinx.coroutines.launch

/**
 * The signed-in shell: bottom navigation + the four destinations. Each screen
 * brings its own top bar / FAB / snackbar; this Scaffold only owns the bottom bar
 * and a top-level snackbar for cross-screen confirmations (e.g. "menu posted").
 */
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    onSwitchToTraveller: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun goTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Reused on every host screen that has a top bar (everything except the
    // chrome-less Today's Menu).
    val modeSwitchAction: @Composable () -> Unit = {
        ModeSwitchButton(label = "Traveller mode", onClick = onSwitchToTraveller)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
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
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.padding(inner),
        ) {
            composable(TopDestination.HOME.route) {
                HomeProfileScreen(
                    onOpenTodaysMenu = { goTo(TopDestination.MENU.route) },
                    trailingTopBarAction = modeSwitchAction,
                )
            }
            composable(TopDestination.MENU.route) {
                DailyMenuScreen(
                    onMenuPublished = {
                        scope.launch { snackbar.showSnackbar("Today's menu is posted ✓") }
                    },
                )
            }
            composable(TopDestination.INQUIRIES.route) {
                InquiryScreen(trailingTopBarAction = modeSwitchAction)
            }
            composable(TopDestination.GUIDE.route) {
                GuideScreen(onSignOut = onSignOut, trailingTopBarAction = modeSwitchAction)
            }
        }
    }
}
