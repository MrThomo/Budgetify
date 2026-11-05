package com.budgetify.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.budgetify.app.R
import com.budgetify.app.core.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onLoggedOut: () -> Unit) {
    val context = LocalContext.current
    var currencyMenuOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.onNotificationPermissionDenied()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.account), style = MaterialTheme.typography.titleSmall)
            Text("${viewModel.accountName}\n${viewModel.accountEmail}", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()

            // Currency
            Text(stringResource(R.string.currency), style = MaterialTheme.typography.titleSmall)
            Box {
                OutlinedButton(onClick = { currencyMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${viewModel.currency} (${Formatters.symbol(viewModel.currency)})")
                }
                DropdownMenu(expanded = currencyMenuOpen, onDismissRequest = { currencyMenuOpen = false }) {
                    Formatters.SUPPORTED_CURRENCIES.forEach { code ->
                        DropdownMenuItem(
                            text = { Text("$code (${Formatters.symbol(code)})") },
                            onClick = {
                                viewModel.onCurrencyChange(code)
                                currencyMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Theme
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dark_theme))
                Switch(checked = viewModel.isDarkTheme, onCheckedChange = viewModel::onThemeToggle)
            }

            // Notifications
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notifications))
                Switch(
                    checked = viewModel.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.onNotificationsToggle(enabled)
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            // Language
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
            listOf(
                "en" to R.string.lang_en,
                "zu" to R.string.lang_zu,
                "st" to R.string.lang_st
            ).forEach { (code, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = viewModel.language == code, onClick = { viewModel.onLanguageChange(code) })
                    Text(stringResource(label))
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_settings))
            }
            viewModel.messageRes?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }

            OutlinedButton(onClick = viewModel::sendTestPush, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.send_test_push))
            }

            HorizontalDivider()
            OutlinedButton(onClick = { viewModel.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.log_out))
            }
        }
    }
}
