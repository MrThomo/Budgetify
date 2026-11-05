package com.budgetify.app.ui.budgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetify.app.R
import com.budgetify.app.core.CategoryStatus
import com.budgetify.app.ui.common.CategoryProgressRow
import com.budgetify.app.ui.common.categoryLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(viewModel: BudgetsViewModel) {
    val state by viewModel.state.collectAsState()
    val currency by viewModel.currency.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryStatus?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.budgets_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.clearError(); showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.statuses, key = { it.categoryId }) { status ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        CategoryProgressRow(status, currency)
                        TextButton(onClick = { viewModel.clearError(); editing = status }) {
                            Text(stringResource(R.string.edit_limit))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var limit by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(stringResource(R.string.add_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 30) name = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text(stringResource(R.string.monthly_limit)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    viewModel.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addCategory(name, limit) { showAdd = false } }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    editing?.let { status ->
        var limit by remember(status.categoryId) { mutableStateOf(status.limit.toString()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("${stringResource(R.string.edit_limit)}: ${categoryLabel(status.name)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text(stringResource(R.string.monthly_limit)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    viewModel.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val entity = state.categories.firstOrNull { it.id == status.categoryId }
                    if (entity != null) viewModel.updateLimit(entity, limit) { editing = null }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
