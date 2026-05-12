package com.ifsvivek.nammahomestay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.ui.MainScreen
import com.ifsvivek.nammahomestay.ui.auth.AuthViewModel
import com.ifsvivek.nammahomestay.ui.auth.LoginScreen
import com.ifsvivek.nammahomestay.ui.theme.NammaHomeStayTheme

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

/** Top-level gate: phone login until signed in, then the bottom-nav shell. */
@Composable
private fun AppRoot() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    if (authState.isLoggedIn) {
        MainScreen(onSignOut = authViewModel::signOut)
    } else {
        LoginScreen(viewModel = authViewModel)
    }
}
