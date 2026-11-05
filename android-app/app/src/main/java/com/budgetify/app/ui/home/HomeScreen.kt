package com.budgetify.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.budgetify.app.R
import com.budgetify.app.core.Formatters
import com.budgetify.app.data.local.TransactionEntity
import com.budgetify.app.ui.common.CategoryProgressRow
import com.budgetify.app.ui.common.categoryLabel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onAddExpense: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val online by viewModel.isOnline.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val categoryNames = state.categories.associate { it.id to it.name }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.hi_user, viewModel.fullName)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_expense))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.spent_this_month), style = MaterialTheme.typography.labelMedium)
                        Text(
                            Formatters.money(state.monthSpent, currency),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text("${stringResource(R.string.points)}: ${state.game.points}", style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.streak_days, state.game.streakDays), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        if (!online) {
                            Text(
                                stringResource(R.string.offline_banner),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (state.pendingCount > 0) stringResource(R.string.pending_sync, state.pendingCount)
                                else stringResource(R.string.all_synced),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (viewModel.isSyncing) {
                                Text(stringResource(R.string.syncing), style = MaterialTheme.typography.labelMedium)
                            } else {
                                TextButton(onClick = viewModel::syncNow, enabled = online) {
                                    Text(stringResource(R.string.sync_now))
                                }
                            }
                        }
                    }
                }
            }

            if (state.statuses.isNotEmpty()) {
                item { Text(stringResource(R.string.category_budgets), style = MaterialTheme.typography.titleMedium) }
                items(state.statuses, key = { it.categoryId }) { status ->
                    CategoryProgressRow(status, currency)
                }
            }

            item { Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleMedium) }

            if (state.transactions.isEmpty()) {
                item { Text(stringResource(R.string.no_transactions)) }
            } else {
                items(state.transactions.take(30), key = { it.localId }) { tx ->
                    TransactionRow(tx, categoryNames[tx.categoryId] ?: "", currency)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity, categoryName: String, currency: String) {
    val imageModel: Any? = tx.receiptPath?.let { File(it) } ?: tx.receiptUrl
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = stringResource(R.string.receipt_attached),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            val title = categoryLabel(categoryName) + if (tx.note.isNotBlank()) " - ${tx.note}" else ""
            Text(title, style = MaterialTheme.typography.bodyLarge)
            val sub = tx.transactionDate + if (!tx.isSynced) "  •  ${stringResource(R.string.waiting_to_sync)}" else ""
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Formatters.money(tx.amount, currency), style = MaterialTheme.typography.bodyLarge)
    }
}
