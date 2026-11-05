package com.budgetify.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.budgetify.app.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showServerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Budgetify", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        viewModel.errorRes?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(it) + (viewModel.errorDetail ?: ""), color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(onLoginSuccess) },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.log_in))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.or_divider), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        // Single sign-on
        OutlinedButton(
            onClick = {
                scope.launch {
                    when (val outcome = requestGoogleIdToken(context)) {
                        is GoogleSignInOutcome.Token -> viewModel.loginWithGoogle(outcome.idToken, onLoginSuccess)
                        is GoogleSignInOutcome.NotConfigured -> viewModel.showError(R.string.sso_not_configured)
                        is GoogleSignInOutcome.Failed -> viewModel.showError(R.string.sso_failed)
                        is GoogleSignInOutcome.Cancelled -> Unit
                    }
                }
            },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.continue_with_google))
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text(stringResource(R.string.no_account))
        }
        TextButton(onClick = { showServerDialog = true }) {
            Text(stringResource(R.string.server_settings), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text(stringResource(R.string.server_settings)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.serverUrl,
                        onValueChange = viewModel::onServerUrlChange,
                        label = { Text(stringResource(R.string.server_url)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    viewModel.serverStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveAndTestServer) { Text(stringResource(R.string.test_connection)) }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }
}
