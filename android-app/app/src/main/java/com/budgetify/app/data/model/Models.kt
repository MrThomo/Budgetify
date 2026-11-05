package com.budgetify.app.data.model

/**
 * Request/response shapes exchanged with the Budgetify REST API.
 * They mirror the DTO records in backend/Budgetify.Api/DTOs.
 */

data class RegisterRequest(val fullName: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleLoginRequest(val idToken: String)

data class AuthResponse(val userId: Int, val fullName: String, val email: String, val token: String)

data class SettingsResponse(
    val currency: String,
    val theme: String,
    val language: String,
    val notificationsEnabled: Boolean
)

data class UpdateSettingsRequest(
    val currency: String? = null,
    val theme: String? = null,
    val language: String? = null,
    val notificationsEnabled: Boolean? = null
)

data class CategoryDto(val id: Int, val name: String, val monthlyLimit: Double)
data class CategoryRequest(val name: String, val monthlyLimit: Double)

data class TransactionDto(
    val id: Int,
    val localId: String?,
    val amount: Double,
    val categoryId: Int,
    val transactionDate: String,
    val note: String?,
    val receiptUrl: String?
)

data class PendingTransactionDto(
    val localId: String,
    val amount: Double,
    val categoryId: Int,
    val transactionDate: String,
    val note: String?,
    val receiptUrl: String?
)

data class SyncRequest(
    val lastSyncTimestamp: String?,
    val pendingTransactions: List<PendingTransactionDto>
)

data class SyncResponse(
    val syncTimestamp: String,
    val syncedLocalIds: List<String>,
    val rejectedLocalIds: List<String>?,
    val serverUpdates: List<TransactionDto>
)

data class DeviceTokenRequest(val token: String)
