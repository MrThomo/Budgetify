package com.budgetify.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetify.app.BuildConfig
import com.budgetify.app.core.InputValidator
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.AppPreferences
import com.budgetify.app.data.repository.AuthRepository
import com.budgetify.app.data.repository.AuthResult
import com.budgetify.app.ui.common.toRes
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val afterLogin: suspend () -> Unit,
    private val prefs: AppPreferences,
    private val api: ApiService
) : ViewModel() {

    var email by mutableStateOf(""); private set
    var password by mutableStateOf(""); private set
    var isLoading by mutableStateOf(false); private set
    var errorRes by mutableStateOf<Int?>(null); private set
    var errorDetail by mutableStateOf<String?>(null); private set

    // ---- Server settings (lets the API address be changed without rebuilding the app) ----
    var serverUrl by mutableStateOf(prefs.apiUrlOverride ?: BuildConfig.API_BASE_URL); private set
    var serverStatus by mutableStateOf<String?>(null); private set

    fun onServerUrlChange(value: String) { serverUrl = value }

    /** Saves the typed address and calls GET / on it to prove the API is reachable. */
    fun saveAndTestServer() {
        prefs.apiUrlOverride = serverUrl.trim().ifEmpty { null }
        serverStatus = "Testing..."
        viewModelScope.launch {
            serverStatus = try {
                val response = api.ping()
                if (response.isSuccessful) "Connected (HTTP ${response.code()}) - the API is running."
                else "The server answered HTTP ${response.code()}. Check the address."
            } catch (e: Exception) {
                "Cannot connect: ${e.javaClass.simpleName}. Check the address and your internet."
            }
        }
    }

    fun onEmailChange(value: String) { email = value }
    fun onPasswordChange(value: String) { password = value }
    fun showError(res: Int) { errorRes = res }

    fun login(onSuccess: () -> Unit) {
        val problem = InputValidator.validateLogin(email, password)
        if (problem != null) {
            errorRes = problem.toRes()
            return
        }
        isLoading = true
        errorRes = null
        errorDetail = null
        viewModelScope.launch { handle(authRepository.login(email.trim(), password), onSuccess) }
    }

    fun loginWithGoogle(idToken: String, onSuccess: () -> Unit) {
        isLoading = true
        errorRes = null
        errorDetail = null
        viewModelScope.launch { handle(authRepository.loginWithGoogle(idToken), onSuccess) }
    }

    private suspend fun handle(result: AuthResult, onSuccess: () -> Unit) {
        when (result) {
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
