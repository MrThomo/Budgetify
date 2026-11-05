package com.budgetify.app.data.api

import com.budgetify.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Every REST endpoint the app consumes on the hosted Budgetify API.
 * Retrofit generates the HTTP implementation at runtime. The JWT "Authorization: Bearer"
 * header is added to every request by the interceptor in [ApiFactory].
 */
interface ApiService {

    /** Health check: GET / returns {"status":"Budgetify API running"} when the server is up. */
    @GET("/")
    suspend fun ping(): Response<Unit>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    /** Single sign-on: exchanges a Google ID token for a Budgetify JWT. */
    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @GET("api/settings")
    suspend fun getSettings(): Response<SettingsResponse>

    @PUT("api/settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequest): Response<SettingsResponse>

    @GET("api/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @POST("api/categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<CategoryDto>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body request: CategoryRequest): Response<CategoryDto>

    @GET("api/transactions")
    suspend fun getTransactions(): Response<List<TransactionDto>>

    /** Offline sync: push locally queued expenses, pull anything newer from the server. */
    @POST("api/transactions/sync")
    suspend fun sync(@Body request: SyncRequest): Response<SyncResponse>

    /** Registers this phone's Firebase Cloud Messaging token so the API can push to it. */
    @POST("api/notifications/token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<Unit>

    /** Asks the API to send a test push notification to this user's phone. */
    @POST("api/notifications/test")
    suspend fun sendTestNotification(): Response<Unit>
}
