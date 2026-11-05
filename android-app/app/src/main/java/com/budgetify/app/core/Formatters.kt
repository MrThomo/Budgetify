package com.budgetify.app.core

import java.util.Locale

object Formatters {

    val SUPPORTED_CURRENCIES = listOf("ZAR", "USD", "EUR", "GBP")

    fun symbol(currency: String): String = when (currency) {
        "ZAR" -> "R"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currency
    }

    /** e.g. money(1234.5, "ZAR") -> "R 1234.50" (Locale.US so the decimal separator is always a dot). */
    fun money(amount: Double, currency: String): String =
        "${symbol(currency)} ${String.format(Locale.US, "%.2f", amount)}"
}
