package com.budgetify.app.ui.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.core.InputValidator
import com.budgetify.app.data.repository.AuthRepository
import com.budgetify.app.data.repository.AuthResult
import com.budgetify.app.ui.common.toRes
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val afterLogin: suspend () -> Unit
) : ViewModel() {

    var fullName by mutableStateOf(""); private set
    var email by mutableStateOf(""); private set
    var password by mutableStateOf(""); private set
    var confirmPassword by mutableStateOf(""); private set
    var isLoading by mutableStateOf(false); private set
    var errorRes by mutableStateOf<Int?>(null); private set
    var errorDetail by mutableStateOf<String?>(null); private set

    fun onFullNameChange(v: String) { fullName = v }
    fun onEmailChange(v: String) { email = v }
    fun onPasswordChange(v: String) { password = v }
    fun onConfirmPasswordChange(v: String) { confirmPassword = v }

    fun register(onSuccess: () -> Unit) {
        val problem = InputValidator.validateRegistration(fullName, email, password, confirmPassword)
        if (problem != null) {
            errorRes = problem.toRes()
            return
        }
        isLoading = true
        errorRes = null
        errorDetail = null
        viewModelScope.launch {
            // The password travels over HTTPS and is hashed with BCrypt on the server (PasswordService.cs).
            when (val result = authRepository.register(fullName.trim(), email.trim(), password)) {
                is AuthResult.Success -> {
                    afterLogin()
                    isLoading = false
                    onSuccess()
                }
                is AuthResult.Failure -> {
                    isLoading = false
                    errorRes = result.reason.toRes()
                    errorDetail = result.httpCode?.let { " (HTTP $it)" }
                }
            }
        }
    }
}
