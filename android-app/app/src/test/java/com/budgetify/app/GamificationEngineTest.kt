package com.budgetify.app

import com.budgetify.app.core.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GamificationEngineTest {

    private val today = LocalDate.of(2026, 11, 4)

    @Test fun noExpenses_noBadgesNoPoints() {
        val state = GamificationEngine.evaluate(emptyList(), emptyList(), today)
        assertEquals(0, state.points)
        assertTrue(state.unlocked.isEmpty())
    }

    @Test fun firstExpense_unlocksFirstStepAndAwardsPoints() {
        val state = GamificationEngine.evaluate(listOf(Expense(1, 10.0, "2026-11-04")), emptyList(), today)
        assertTrue(Badge.FIRST_EXPENSE in state.unlocked)
        assertEquals(10, state.points)
    }

    @Test fun receipts_awardBonusPointsAndBadge() {
        val state = GamificationEngine.evaluate(listOf(Expense(1, 10.0, "2026-11-04", hasReceipt = true)), emptyList(), today)
        assertEquals(15, state.points)
        assertTrue(Badge.RECEIPT_KEEPER in state.unlocked)
    }

    @Test fun streak_countsConsecutiveDaysEndingToday() {
        val dates = listOf("2026-11-04", "2026-11-03", "2026-11-02", "2026-10-30")
        assertEquals(3, GamificationEngine.currentStreak(dates, today))
    }

    @Test fun streak_survivesIfTodayNotYetLogged() {
        assertEquals(2, GamificationEngine.currentStreak(listOf("2026-11-03", "2026-11-02"), today))
    }

    @Test fun streak_resetsAfterGap() {
        assertEquals(0, GamificationEngine.currentStreak(listOf("2026-11-01"), today))
    }

    @Test fun streak_ignoresMalformedDates() {
        assertEquals(1, GamificationEngine.currentStreak(listOf("2026-11-04", "garbage", ""), today))
    }

    @Test fun sevenDayStreak_unlocksBothStreakBadges() {
        val expenses = (0..6).map { Expense(1, 5.0, today.minusDays(it.toLong()).toString()) }
        val state = GamificationEngine.evaluate(expenses, emptyList(), today)
        assertTrue(Badge.STREAK_3 in state.unlocked)
        assertTrue(Badge.STREAK_7 in state.unlocked)
        assertTrue(Badge.FIVE_EXPENSES in state.unlocked)
    }

    @Test fun budgetKeeper_requiresNoCategoryOverLimit() {
        val expenses = listOf(Expense(1, 10.0, "2026-11-04"))
        val ok = listOf(CategoryStatus(1, "A", 100.0, 10.0))
        val over = listOf(CategoryStatus(1, "A", 5.0, 10.0))
        assertTrue(Badge.BUDGET_KEEPER in GamificationEngine.evaluate(expenses, ok, today).unlocked)
        assertFalse(Badge.BUDGET_KEEPER in GamificationEngine.evaluate(expenses, over, today).unlocked)
    }
}
