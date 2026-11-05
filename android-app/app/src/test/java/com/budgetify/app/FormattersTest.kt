package com.budgetify.app

import com.budgetify.app.core.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test fun money_usesSymbolAndTwoDecimals() {
        assertEquals("R 1234.50", Formatters.money(1234.5, "ZAR"))
        assertEquals("$ 0.99", Formatters.money(0.99, "USD"))
    }

    @Test fun unknownCurrency_fallsBackToCode() {
        assertEquals("XYZ 5.00", Formatters.money(5.0, "XYZ"))
    }
}
