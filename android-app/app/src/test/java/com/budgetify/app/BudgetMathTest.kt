package com.budgetify.app

import com.budgetify.app.core.BudgetMath
import com.budgetify.app.core.CategoryInfo
import com.budgetify.app.core.CategoryStatus
import com.budgetify.app.core.Expense
import org.junit.Assert.*
import org.junit.Test

class BudgetMathTest {

    private val expenses = listOf(
        Expense(1, 120.50, "2026-11-02"),
        Expense(1, 45.00, "2026-11-03"),
        Expense(2, 9.99, "2026-11-03"),
        Expense(1, 500.00, "2026-10-30") // previous month, must be ignored
    )

    @Test fun spentInMonth_onlyCountsThatMonth() {
        assertEquals(175.49, BudgetMath.spentInMonth(expenses, "2026-11"), 0.001)
    }

    @Test fun categoryStatuses_sumPerCategory() {
        val cats = listOf(CategoryInfo(1, "Groceries", 200.0), CategoryInfo(2, "Transport", 100.0))
        val statuses = BudgetMath.categoryStatuses(cats, expenses, "2026-11")
        assertEquals(165.50, statuses[0].spent, 0.001)
        assertEquals(9.99, statuses[1].spent, 0.001)
        assertFalse(statuses[0].isOver)
    }

    @Test fun status_percentAndOverBudget() {
        val s = CategoryStatus(1, "Fun", 100.0, 125.0)
        assertTrue(s.isOver)
        assertEquals(125, s.percent)
        assertFalse(CategoryStatus(1, "Fun", 0.0, 50.0).isOver) // no limit set => never "over"
    }

    @Test fun crossedThreshold_detectsWarningAndLimit() {
        assertEquals(80, BudgetMath.crossedThreshold(70.0, 85.0, 100.0))
        assertEquals(100, BudgetMath.crossedThreshold(90.0, 101.0, 100.0))
        assertEquals(100, BudgetMath.crossedThreshold(10.0, 150.0, 100.0)) // jumps over both lines
    }

    @Test fun crossedThreshold_returnsNullWhenNothingNewCrossed() {
        assertNull(BudgetMath.crossedThreshold(10.0, 20.0, 100.0))
        assertNull(BudgetMath.crossedThreshold(85.0, 90.0, 100.0)) // already past 80%
        assertNull(BudgetMath.crossedThreshold(110.0, 120.0, 100.0)) // already over
        assertNull(BudgetMath.crossedThreshold(0.0, 50.0, 0.0))
    }
}
