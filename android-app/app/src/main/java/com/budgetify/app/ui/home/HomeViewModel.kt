package com.budgetify.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.repository.BudgetRepository
import com.budgetify.app.data.repository.BudgetSnapshot
import com.budgetify.app.sync.NetworkMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: BudgetRepository,
    session: SessionManager,
    private val network: NetworkMonitor,
    prefs: AppPreferences
) : ViewModel() {

    val fullName: String = session.getFullName() ?: ""

    val state: StateFlow<BudgetSnapshot> = repo.observeSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetSnapshot())

    val isOnline: StateFlow<Boolean> = network.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), network.isOnlineNow())

    val currency: StateFlow<String> = prefs.currencyFlow

    var isSyncing by mutableStateOf(false); private set

    init {
        // Whenever connectivity (re)appears, push offline changes straight away.
        viewModelScope.launch {
            network.isOnline.collect { online -> if (online) syncNow() }
        }
    }

    fun syncNow() {
        if (isSyncing) return
        isSyncing = true
        viewModelScope.launch {
            repo.syncNow()
            isSyncing = false
        }
    }
}
