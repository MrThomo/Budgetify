package com.budgetify.app.ui.rewards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.budgetify.app.R
import com.budgetify.app.core.Badge
import com.budgetify.app.ui.common.descRes
import com.budgetify.app.ui.common.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(viewModel: RewardsViewModel) {
    val state by viewModel.state.collectAsState()
    val unlocked = state.game.unlocked + viewModel.cloudBadges

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.rewards_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.total_points), style = MaterialTheme.typography.labelMedium)
                        Text("${state.game.points}", style = MaterialTheme.typography.displaySmall)
                        Text(stringResource(R.string.streak_days, state.game.streakDays))
                        if (viewModel.cloudBadges.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.cloud_synced), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(Badge.entries.toList(), key = { it.key }) { badge ->
                val isUnlocked = badge in unlocked
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isUnlocked) Icons.Filled.Star else Icons.Filled.Lock,
                            contentDescription = stringResource(if (isUnlocked) R.string.unlocked else R.string.locked),
                            tint = if (isUnlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(stringResource(badge.titleRes()), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(badge.descRes()), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
