package com.budgetify.app.core

/** A single expense, stripped down to what the budget maths needs. */
data class Expense(
    val categoryId: Int,
    val amount: Double,
    /** ISO date, yyyy-MM-dd */
    val date: String,
    val hasReceipt: Boolean = false
)

data class CategoryInfo(val id: Int, val name: String, val monthlyLimit: Double)

/** How much of one category's monthly limit has been used. */
data class CategoryStatus(
    val categoryId: Int,
    val name: String,
    val limit: Double,
    val spent: Double
) {
    val fraction: Double get() = if (limit <= 0.0) 0.0 else spent / limit
    val percent: Int get() = (fraction * 100).toInt()
    val isOver: Boolean get() = limit > 0.0 && spent > limit
}

object BudgetMath {

    /** "2026-11-04" -> "2026-11" */
    fun yearMonth(isoDate: String): String = isoDate.take(7)

    fun spentInMonth(expenses: List<Expense>, yearMonth: String): Double =
        expenses.filter { it.date.startsWith(yearMonth) }.sumOf { it.amount }

    fun categoryStatuses(
        categories: List<CategoryInfo>,
        expenses: List<Expense>,
        yearMonth: String
    ): List<CategoryStatus> = categories.map { c ->
        val spent = expenses
            .filter { it.categoryId == c.id && it.date.startsWith(yearMonth) }
            .sumOf { it.amount }
        CategoryStatus(c.id, c.name, c.monthlyLimit, spent)
    }

    /**
     * Did adding an expense push a category over the 80% warning line or the 100% limit?
     * Returns 100, 80, or null (nothing crossed). Crossing both at once reports 100.
     * The same rule is implemented server-side (BudgetRules.cs) so push alerts agree.
     */
    fun crossedThreshold(before: Double, after: Double, limit: Double): Int? {
        if (limit <= 0.0) return null
        return when {
            before <= limit && after > limit -> 100
            before < limit * 0.8 && after >= limit * 0.8 -> 80
            else -> null
        }
    }
}
