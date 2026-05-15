package com.ifsvivek.nammahomestay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.data.role.UserMode
import com.ifsvivek.nammahomestay.ui.MainScreen
import com.ifsvivek.nammahomestay.ui.auth.AuthViewModel
import com.ifsvivek.nammahomestay.ui.auth.LoginScreen
import com.ifsvivek.nammahomestay.ui.role.ModePickerScreen
import com.ifsvivek.nammahomestay.ui.role.ModeState
import com.ifsvivek.nammahomestay.ui.role.RoleViewModel
import com.ifsvivek.nammahomestay.ui.theme.NammaHomeStayTheme
import com.ifsvivek.nammahomestay.ui.traveller.TravellerMainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NammaHomeStayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * Top-level gate. Three concentric checks decide what's on screen:
 *   1. Not signed in  → [LoginScreen]
 *   2. Signed in, no mode chosen yet → [ModePickerScreen]
 *   3. Signed in + mode chosen → [MainScreen] (host) or [TravellerMainScreen]
 *
 * On sign-out, [RoleViewModel.clearOnSignOut] resets the persisted mode so the
 * next user lands on the picker.
 */
@Composable
private fun AppRoot() {
    val authViewModel: AuthViewModel = viewModel()
    val roleViewModel: RoleViewModel = viewModel()

    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val modeState by roleViewModel.state.collectAsStateWithLifecycle()

    // Clear the persisted mode whenever the user signs out, so the next sign-in
    // (potentially by a different person) gets the welcome picker.
    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) roleViewModel.clearOnSignOut()
    }

    val signOut = {
        authViewModel.signOut()
    }

    when {
        !authState.isLoggedIn -> LoginScreen(viewModel = authViewModel)
        modeState is ModeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        modeState is ModeState.NotChosen -> ModePickerScreen(roleViewModel = roleViewModel)
        modeState is ModeState.Chosen -> when ((modeState as ModeState.Chosen).mode) {
            UserMode.HOST -> MainScreen(
                onSignOut = signOut,
                onSwitchToTraveller = { roleViewModel.switchTo(UserMode.TRAVELLER) },
            )
            UserMode.TRAVELLER -> TravellerMainScreen(
                onSwitchToHost = { roleViewModel.switchTo(UserMode.HOST) },
                onSignOut = signOut,
            )
        }
    }
}
