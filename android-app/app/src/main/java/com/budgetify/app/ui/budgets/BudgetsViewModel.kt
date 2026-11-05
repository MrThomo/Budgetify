package com.budgetify.app.ui.budgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.R
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.local.CategoryEntity
import com.budgetify.app.data.repository.BudgetRepository
import com.budgetify.app.data.repository.BudgetSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetsViewModel(
    private val repo: BudgetRepository,
    prefs: AppPreferences
) : ViewModel() {

    val state: StateFlow<BudgetSnapshot> = repo.observeSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetSnapshot())

    val currency: StateFlow<String> = prefs.currencyFlow

    var errorRes by mutableStateOf<Int?>(null); private set

    init {
        viewModelScope.launch { repo.refreshCategories() }
    }

    /** Limits are whole or 2-decimal positive numbers, same rule as expense amounts. */
    private fun parseLimit(text: String): Double? =
        com.budgetify.app.core.InputValidator.parseAmount(text)

    fun addCategory(name: String, limitText: String, onDone: () -> Unit) {
        val limit = parseLimit(limitText)
        when {
            name.isBlank() -> { errorRes = R.string.err_category_name; return }
            limit == null -> { errorRes = R.string.err_limit_invalid; return }
        }
        errorRes = null
        viewModelScope.launch {
            if (repo.addCategory(name, limit!!)) onDone() else errorRes = R.string.err_network
        }
    }

    fun updateLimit(category: CategoryEntity, limitText: String, onDone: () -> Unit) {
        val limit = parseLimit(limitText)
        if (limit == null) {
            errorRes = R.string.err_limit_invalid
            return
        }
        errorRes = null
        viewModelScope.launch {
            if (repo.updateCategoryLimit(category, limit)) onDone() else errorRes = R.string.err_network
        }
    }

    fun clearError() { errorRes = null }
}
