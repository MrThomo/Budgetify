package com.budgetify.app.ui.rewards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.core.Badge
import com.budgetify.app.data.repository.BudgetRepository
import com.budgetify.app.data.repository.BudgetSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardsViewModel(private val repo: BudgetRepository) : ViewModel() {

    val state: StateFlow<BudgetSnapshot> = repo.observeSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetSnapshot())

    /** Badges read back from Firestore (NoSQL). Merged with the locally computed ones in the UI. */
    var cloudBadges by mutableStateOf<Set<Badge>>(emptySet()); private set

    init {
        viewModelScope.launch {
            repo.publishAchievements()
            cloudBadges = repo.loadCloudBadges()
        }
    }
}
