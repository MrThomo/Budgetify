package com.budgetify.app.data.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit [ApiService]. Kept as a plain factory (no Android classes) so unit
 * tests can point it at a MockWebServer.
 */
object ApiFactory {

    fun normalizeBaseUrl(url: String): String = url.trim().trimEnd('/') + "/"

    /**
     * @param baseUrlOverride optional server address chosen at runtime (Login -> Server settings).
     *        When it returns a valid URL, every request is redirected to that host, so the API
     *        address can be changed without rebuilding the app.
     * @param onUnauthorized called when a request that carried a token gets HTTP 401 (expired login).
     */
    fun create(
        baseUrl: String,
        tokenProvider: () -> String?,
        debug: Boolean,
        onUnauthorized: () -> Unit = {},
        baseUrlOverride: () -> String? = { null }
    ): ApiService {
        // BASIC logs method, URL and status code only - never request bodies, so passwords
        // and tokens are not written to Logcat.
        val logging = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val appInterceptor = Interceptor { chain ->
            var request = chain.request()

            val override = baseUrlOverride()?.trim()?.takeIf { it.isNotEmpty() }
                ?.let { runCatching { normalizeBaseUrl(it).toHttpUrl() }.getOrNull() }
            if (override != null) {
                val redirected = request.url.newBuilder()
                    .scheme(override.scheme)
                    .host(override.host)
                    .port(override.port)
                    .build()
                request = request.newBuilder().url(redirected).build()
            }

            val token = tokenProvider()
            if (!token.isNullOrBlank()) {
                request = request.newBuilder().header("Authorization", "Bearer $token").build()
            }

            val response = chain.proceed(request)
            // A 401 on a request that carried a token means the JWT expired (or was revoked).
            if (response.code == 401 && !token.isNullOrBlank()) onUnauthorized()
            response
        }

        // Generous timeouts: a free-tier cloud host can take ~60s to wake from sleep.
        val client = OkHttpClient.Builder()
            .addInterceptor(appInterceptor)
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
