package com.budgetify.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.budgetify.app.R
import com.budgetify.app.core.Badge
import com.budgetify.app.core.CategoryNames
import com.budgetify.app.core.CategoryStatus
import com.budgetify.app.core.Formatters
import com.budgetify.app.core.InputValidator
import com.budgetify.app.data.repository.AuthFailure

/**
 * Small generic factory so ViewModels can take constructor dependencies (repositories)
 * without pulling in a full DI framework.
 */
class ViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = creator() as V
}

fun InputValidator.AuthError.toRes(): Int = when (this) {
    InputValidator.AuthError.REQUIRED -> R.string.err_required_all
    InputValidator.AuthError.EMAIL_INVALID -> R.string.err_email_invalid
    InputValidator.AuthError.PASSWORD_WEAK -> R.string.err_password_weak
    InputValidator.AuthError.PASSWORD_MISMATCH -> R.string.err_password_mismatch
}

fun AuthFailure.toRes(): Int = when (this) {
    AuthFailure.INVALID_CREDENTIALS -> R.string.err_invalid_credentials
    AuthFailure.EMAIL_TAKEN -> R.string.err_email_taken
    AuthFailure.NETWORK -> R.string.err_network
    AuthFailure.SERVER_NOT_FOUND -> R.string.err_server_not_found
    AuthFailure.GENERIC -> R.string.err_generic
}

fun Badge.titleRes(): Int = when (this) {
    Badge.FIRST_EXPENSE -> R.string.badge_first_expense
    Badge.FIVE_EXPENSES -> R.string.badge_five_expenses
    Badge.STREAK_3 -> R.string.badge_streak_3
    Badge.STREAK_7 -> R.string.badge_streak_7
    Badge.BUDGET_KEEPER -> R.string.badge_budget_keeper
    Badge.RECEIPT_KEEPER -> R.string.badge_receipt_keeper
}

fun Badge.descRes(): Int = when (this) {
    Badge.FIRST_EXPENSE -> R.string.badge_first_expense_desc
    Badge.FIVE_EXPENSES -> R.string.badge_five_expenses_desc
    Badge.STREAK_3 -> R.string.badge_streak_3_desc
    Badge.STREAK_7 -> R.string.badge_streak_7_desc
    Badge.BUDGET_KEEPER -> R.string.badge_budget_keeper_desc
    Badge.RECEIPT_KEEPER -> R.string.badge_receipt_keeper_desc
}

/** Translated label for the seeded default categories; custom categories keep their own name. */
@Composable
fun categoryLabel(name: String): String =
    CategoryNames.resFor(name)?.let { stringResource(it) } ?: name

@Composable
fun CategoryProgressRow(status: CategoryStatus, currency: String, modifier: Modifier = Modifier) {
    val barColor = when {
        status.isOver -> MaterialTheme.colorScheme.error
        status.percent >= 80 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(categoryLabel(status.name), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.of_limit,
                    Formatters.money(status.spent, currency),
                    Formatters.money(status.limit, currency)
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { status.fraction.coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor
        )
        if (status.isOver) {
            Text(
                stringResource(R.string.over_budget),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
