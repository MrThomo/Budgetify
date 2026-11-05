package com.budgetify.app.data.repository

import android.util.Log
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.local.SessionManager
import com.budgetify.app.data.model.AuthResponse
import com.budgetify.app.data.model.GoogleLoginRequest
import com.budgetify.app.data.model.LoginRequest
import com.budgetify.app.data.model.RegisterRequest
import retrofit2.Response

enum class AuthFailure { INVALID_CREDENTIALS, EMAIL_TAKEN, NETWORK, SERVER_NOT_FOUND, GENERIC }

sealed class AuthResult {
    data class Success(val response: AuthResponse) : AuthResult()
    data class Failure(val reason: AuthFailure, val httpCode: Int? = null) : AuthResult()
}

/**
 * Mediates between the UI and the REST API for authentication. On success the returned JWT
 * is persisted by [SessionManager] so the user stays logged in. Passwords are sent over
 * HTTPS and hashed with BCrypt on the server - they are never stored in plain text.
 */
class AuthRepository(
    private val api: ApiService,
    private val sessionManager: SessionManager,
    private val cloud: CloudRepository
) {
    private val tag = "AuthRepository"

    suspend fun register(fullName: String, email: String, password: String): AuthResult =
        call("register") { api.register(RegisterRequest(fullName, email, password)) }

    suspend fun login(email: String, password: String): AuthResult =
        call("login") { api.login(LoginRequest(email, password)) }

    /** Single sign-on: Google ID token -> Budgetify JWT (and a Firebase Auth identity). */
    suspend fun loginWithGoogle(idToken: String): AuthResult {
        val result = call("google login") { api.googleLogin(GoogleLoginRequest(idToken)) }
        if (result is AuthResult.Success) cloud.signInWithGoogle(idToken)
        return result
    }

    private suspend fun call(label: String, block: suspend () -> Response<AuthResponse>): AuthResult {
        return try {
            val response = block()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                sessionManager.saveSession(body.userId, body.fullName, body.email, body.token)
                Log.i(tag, "$label successful for user ${body.userId}")
                AuthResult.Success(body)
            } else {
                Log.w(tag, "$label failed with HTTP ${response.code()}")
                AuthResult.Failure(
                    when (response.code()) {
                        401 -> AuthFailure.INVALID_CREDENTIALS
                        409 -> AuthFailure.EMAIL_TAKEN
                        404 -> AuthFailure.SERVER_NOT_FOUND
                        else -> AuthFailure.GENERIC
                    },
                    response.code()
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "$label network error", e)
            AuthResult.Failure(AuthFailure.NETWORK)
        }
    }

    fun logout() {
        sessionManager.clearSession()
        cloud.signOut()
        Log.i(tag, "User logged out")
    }
}
