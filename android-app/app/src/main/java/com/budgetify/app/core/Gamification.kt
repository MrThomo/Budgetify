package com.budgetify.app.core

import java.time.LocalDate

/** Achievements a user can unlock (the "gamified budget tracking" feature from Part 1). */
enum class Badge(val key: String) {
    FIRST_EXPENSE("first_expense"),
    FIVE_EXPENSES("five_expenses"),
    STREAK_3("streak_3"),
    STREAK_7("streak_7"),
    BUDGET_KEEPER("budget_keeper"),
    RECEIPT_KEEPER("receipt_keeper");

    companion object {
        fun fromKey(key: String): Badge? = entries.firstOrNull { it.key == key }
    }
}

data class GameState(
    val points: Int = 0,
    val streakDays: Int = 0,
    val unlocked: Set<Badge> = emptySet()
)

object GamificationEngine {

    const val POINTS_PER_EXPENSE = 10
    const val POINTS_PER_RECEIPT = 5

    /**
     * Consecutive days (ending today, or yesterday if nothing logged yet today) on which
     * at least one expense was logged. Dates are ISO yyyy-MM-dd strings; bad ones are ignored.
     */
    fun currentStreak(dates: Collection<String>, today: LocalDate): Int {
        val days = dates.mapNotNull { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }.toSet()
        var cursor = if (days.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun evaluate(
        expenses: List<Expense>,
        statuses: List<CategoryStatus>,
        today: LocalDate
    ): GameState {
        val streak = currentStreak(expenses.map { it.date }, today)
        val receipts = expenses.count { it.hasReceipt }
        val points = expenses.size * POINTS_PER_EXPENSE + receipts * POINTS_PER_RECEIPT

        val thisMonth = BudgetMath.yearMonth(today.toString())
        val hasExpenseThisMonth = expenses.any { it.date.startsWith(thisMonth) }

        val unlocked = buildSet {
            if (expenses.isNotEmpty()) add(Badge.FIRST_EXPENSE)
            if (expenses.size >= 5) add(Badge.FIVE_EXPENSES)
            if (streak >= 3) add(Badge.STREAK_3)
            if (streak >= 7) add(Badge.STREAK_7)
            if (receipts >= 1) add(Badge.RECEIPT_KEEPER)
            if (hasExpenseThisMonth && statuses.none { it.isOver }) add(Badge.BUDGET_KEEPER)
        }
        return GameState(points, streak, unlocked)
    }
}
