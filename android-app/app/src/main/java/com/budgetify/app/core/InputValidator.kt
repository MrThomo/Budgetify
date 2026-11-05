package com.budgetify.app.core

/**
 * Pure (Android-free) input validation so the exact same rules used by the ViewModels
 * can be unit-tested on the JVM. Invalid input is rejected here before it ever reaches
 * the REST API, which keeps the UI from crashing or sending junk to the server.
 */
object InputValidator {

    enum class AuthError { REQUIRED, EMAIL_INVALID, PASSWORD_WEAK, PASSWORD_MISMATCH }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val AMOUNT_REGEX = Regex("^\\d{1,9}([.,]\\d{1,2})?$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /** At least 8 characters, containing at least one letter and one digit. */
    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun validateLogin(email: String, password: String): AuthError? = when {
        email.isBlank() || password.isBlank() -> AuthError.REQUIRED
        !isValidEmail(email) -> AuthError.EMAIL_INVALID
        else -> null
    }

    fun validateRegistration(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthError? = when {
        fullName.isBlank() || email.isBlank() || password.isBlank() -> AuthError.REQUIRED
        !isValidEmail(email) -> AuthError.EMAIL_INVALID
        !isStrongPassword(password) -> AuthError.PASSWORD_WEAK
        password != confirmPassword -> AuthError.PASSWORD_MISMATCH
        else -> null
    }

    /**
     * Parses a user-typed money amount. Accepts "12.50" or "12,50" (South African keyboards
     * often use a decimal comma). Returns null for anything that is not a positive amount
     * with at most two decimals, e.g. "", "abc", "-5", "1e5", "0", "12.345".
     */
    fun parseAmount(text: String): Double? {
        val cleaned = text.trim()
        if (!AMOUNT_REGEX.matches(cleaned)) return null
        val value = cleaned.replace(',', '.').toDoubleOrNull() ?: return null
        return if (value > 0.0) value else null
    }
}
