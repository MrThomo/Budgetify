package com.budgetify.app.ui.addexpense

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.R
import com.budgetify.app.core.InputValidator
import com.budgetify.app.data.local.CategoryEntity
import com.budgetify.app.data.repository.BudgetRepository
import com.budgetify.app.data.repository.SaveOutcome
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AddExpenseViewModel(private val repo: BudgetRepository) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repo.observeSnapshot()
        .map { it.categories }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var amountText by mutableStateOf(""); private set
    var selectedCategoryId by mutableStateOf<Int?>(null); private set
    var note by mutableStateOf(""); private set
    var date by mutableStateOf(LocalDate.now().toString()); private set
    var receiptUri by mutableStateOf<Uri?>(null); private set
    var isSaving by mutableStateOf(false); private set
    var errorRes by mutableStateOf<Int?>(null); private set

    init {
        // Make sure categories are available even on a freshly installed phone.
        viewModelScope.launch { repo.refreshCategories() }
    }

    fun onAmountChange(value: String) {
        // Only digits and a decimal separator are accepted, which blocks junk input at the keyboard level.
        if (value.length <= 12 && value.all { it.isDigit() || it == '.' || it == ',' }) amountText = value
    }

    fun onCategorySelected(id: Int) { selectedCategoryId = id }
    fun onNoteChange(value: String) { if (value.length <= 80) note = value }
    fun onReceiptPicked(uri: Uri?) { if (uri != null) receiptUri = uri }

    fun dateMillis(): Long =
        LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun onDateMillisSelected(millis: Long) {
        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
    }

    fun save(onSaved: (SaveOutcome) -> Unit) {
        val amount = InputValidator.parseAmount(amountText)
        val categoryId = selectedCategoryId
        when {
            amount == null -> { errorRes = R.string.err_amount_invalid; return }
            categoryId == null -> { errorRes = R.string.err_category_required; return }
        }
        errorRes = null
        isSaving = true
        viewModelScope.launch {
            val outcome = repo.addExpense(amount!!, categoryId!!, date, note, receiptUri)
            isSaving = false
            onSaved(outcome)
        }
    }
}
