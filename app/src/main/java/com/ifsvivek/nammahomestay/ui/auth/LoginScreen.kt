package com.ifsvivek.nammahomestay.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ifsvivek.nammahomestay.R
import com.ifsvivek.nammahomestay.ui.components.BigActionButton
import com.ifsvivek.nammahomestay.util.findActivity

/**
 * The whole sign-in: one phone number, one 6-digit code. No passwords, no email.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Filled.Cottage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        when (state.step) {
            LoginStep.ENTER_PHONE -> PhoneStep(state, viewModel, context)
            LoginStep.ENTER_CODE -> CodeStep(state, viewModel)
        }

        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                state.error!!,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhoneStep(
    state: LoginUiState,
    viewModel: AuthViewModel,
    context: android.content.Context,
) {
    Text(
        stringResource(R.string.login_enter_phone),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = state.phoneDigits,
        onValueChange = viewModel::onPhoneChange,
        label = { Text(stringResource(R.string.login_phone_label)) },
        prefix = { Text("+91 ") },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.login_send_sms_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        BigActionButton(
            text = stringResource(R.string.login_send_code),
            enabled = state.phoneValid && !state.busy,
            onClick = { context.findActivity()?.let(viewModel::requestCode) },
        )
        if (state.sending) CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
private fun CodeStep(state: LoginUiState, viewModel: AuthViewModel) {
    Text(
        stringResource(R.string.login_enter_code_sent_to),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        state.e164Phone,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = state.code,
        onValueChange = viewModel::onCodeChange,
        label = { Text(stringResource(R.string.login_code_label)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        BigActionButton(
            text = stringResource(R.string.login_verify_continue),
            enabled = state.codeValid && !state.busy,
            onClick = viewModel::submitCode,
        )
        if (state.verifying) CircularProgressIndicator(strokeWidth = 3.dp)
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = viewModel::backToPhoneStep) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.login_change_number), style = MaterialTheme.typography.bodyLarge)
    }
}
