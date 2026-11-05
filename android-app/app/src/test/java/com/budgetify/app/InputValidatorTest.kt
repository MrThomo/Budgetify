package com.budgetify.app

import com.budgetify.app.core.InputValidator
import com.budgetify.app.core.InputValidator.AuthError
import org.junit.Assert.*
import org.junit.Test

/** Tests the REAL validation object used by the Login/Register/AddExpense ViewModels. */
class InputValidatorTest {

    @Test fun validRegistration_returnsNoError() {
        assertNull(InputValidator.validateRegistration("Jane Doe", "jane@example.com", "secret123", "secret123"))
    }

    @Test fun blankFields_areRejected() {
        assertEquals(AuthError.REQUIRED, InputValidator.validateRegistration("", "jane@example.com", "secret123", "secret123"))
        assertEquals(AuthError.REQUIRED, InputValidator.validateLogin("  ", "x"))
    }

    @Test fun badEmail_isRejected() {
        assertEquals(AuthError.EMAIL_INVALID, InputValidator.validateLogin("not-an-email", "secret123"))
        assertFalse(InputValidator.isValidEmail("a@b"))
        assertTrue(InputValidator.isValidEmail(" user.name+tag@mail.co.za "))
    }

    @Test fun weakPassword_isRejected() {
        assertEquals(AuthError.PASSWORD_WEAK, InputValidator.validateRegistration("J", "j@x.com", "short1", "short1"))
        assertEquals(AuthError.PASSWORD_WEAK, InputValidator.validateRegistration("J", "j@x.com", "onlyletters", "onlyletters"))
        assertEquals(AuthError.PASSWORD_WEAK, InputValidator.validateRegistration("J", "j@x.com", "12345678", "12345678"))
    }

    @Test fun mismatchedPasswords_areRejected() {
        assertEquals(AuthError.PASSWORD_MISMATCH, InputValidator.validateRegistration("J", "j@x.com", "secret123", "secret124"))
    }

    @Test fun parseAmount_acceptsDotAndCommaDecimals() {
        assertEquals(12.5, InputValidator.parseAmount("12.50")!!, 0.0001)
        assertEquals(12.5, InputValidator.parseAmount("12,5")!!, 0.0001)
        assertEquals(100.0, InputValidator.parseAmount(" 100 ")!!, 0.0001)
    }

    @Test fun parseAmount_rejectsInvalidInput() {
        listOf("", "abc", "-5", "0", "0.00", "1e5", "12.345", "1.2.3", "1,000.00", "9999999999").forEach {
            assertNull("'$it' should be rejected", InputValidator.parseAmount(it))
        }
    }
}
