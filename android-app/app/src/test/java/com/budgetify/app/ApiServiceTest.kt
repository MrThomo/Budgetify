package com.budgetify.app

import com.budgetify.app.data.api.ApiFactory
import com.budgetify.app.data.api.ApiService
import com.budgetify.app.data.model.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests the Retrofit REST client against a local MockWebServer: correct URLs, HTTP verbs,
 * JSON bodies, bearer-token header and response parsing - no real network needed.
 */
class ApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    private var token: String? = null

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiFactory.create(server.url("/").toString(), { token }, debug = false)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun login_postsCredentials_andParsesToken() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("""{"userId":7,"fullName":"Jane","email":"j@x.com","token":"abc.def"}"""))

        val response = api.login(LoginRequest("j@x.com", "Secret123"))
        val recorded = server.takeRequest()

        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/login", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"email\":\"j@x.com\""))
        assertNull("login must not send a bearer header when logged out", recorded.getHeader("Authorization"))
        assertTrue(response.isSuccessful)
        assertEquals("abc.def", response.body()?.token)
        assertEquals(7, response.body()?.userId)
    }

    @Test fun login_wrongPassword_returns401() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Invalid email or password."}"""))
        val response = api.login(LoginRequest("j@x.com", "bad"))
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code())
    }

    @Test fun protectedEndpoints_sendBearerToken() = runBlocking {
        token = "my-jwt"
        server.enqueue(MockResponse().setBody("""{"currency":"ZAR","theme":"dark","language":"zu","notificationsEnabled":true}"""))

        val settings = api.getSettings().body()
        val recorded = server.takeRequest()

        assertEquals("GET", recorded.method)
        assertEquals("/api/settings", recorded.path)
        assertEquals("Bearer my-jwt", recorded.getHeader("Authorization"))
        assertEquals("zu", settings?.language)
        assertEquals(true, settings?.notificationsEnabled)
    }

    @Test fun updateSettings_putsOnlyProvidedFields() = runBlocking {
        token = "t"
        server.enqueue(MockResponse().setBody("""{"currency":"USD","theme":"light","language":"en","notificationsEnabled":false}"""))
        api.updateSettings(UpdateSettingsRequest(currency = "USD"))
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertTrue(recorded.body.readUtf8().contains("\"currency\":\"USD\""))
    }

    @Test fun sync_sendsPendingAndParsesServerUpdates() = runBlocking {
        token = "t"
        server.enqueue(MockResponse().setBody(
            """{"syncTimestamp":"2026-11-04T10:00:00Z","syncedLocalIds":["abc"],"rejectedLocalIds":[],
               "serverUpdates":[{"id":5,"localId":"abc","amount":99.5,"categoryId":2,
               "transactionDate":"2026-11-04T00:00:00","note":"Lunch","receiptUrl":null}]}"""))

        val response = api.sync(
            SyncRequest(null, listOf(PendingTransactionDto("abc", 99.5, 2, "2026-11-04T00:00:00", "Lunch", null)))
        )
        val recorded = server.takeRequest()
        val body = response.body()!!

        assertEquals("/api/transactions/sync", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"localId\":\"abc\""))
        assertEquals(listOf("abc"), body.syncedLocalIds)
        assertEquals(1, body.serverUpdates.size)
        assertEquals(99.5, body.serverUpdates[0].amount, 0.001)
        assertEquals("2026-11-04", body.serverUpdates[0].transactionDate.take(10))
    }

    @Test fun expiredToken_401_triggersUnauthorizedCallback() = runBlocking {
        var called = false
        token = "old"
        val api2 = ApiFactory.create(server.url("/").toString(), { token }, false, onUnauthorized = { called = true })
        server.enqueue(MockResponse().setResponseCode(401))
        api2.getSettings()
        assertTrue(called)
    }

    @Test fun login401_withoutToken_doesNotTriggerCallback() = runBlocking {
        var called = false
        token = null
        val api2 = ApiFactory.create(server.url("/").toString(), { token }, false, onUnauthorized = { called = true })
        server.enqueue(MockResponse().setResponseCode(401))
        api2.login(LoginRequest("a@b.com", "x"))
        assertFalse(called)
    }

    @Test fun ping_hitsRoot() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"status":"Budgetify API running"}"""))
        val response = api.ping()
        assertEquals("/", server.takeRequest().path)
        assertTrue(response.isSuccessful)
    }

    @Test fun baseUrlOverride_redirectsRequestsToChosenServer() = runBlocking {
        // The app is "built" for an unreachable address, but the user typed the real one in Server settings.
        val redirected = ApiFactory.create(
            "https://not-a-real-host.invalid/", { null }, false,
            baseUrlOverride = { server.url("/").toString() }
        )
        server.enqueue(MockResponse().setBody("{}"))
        redirected.ping()
        assertEquals(1, server.requestCount)
    }

    @Test fun baseUrl_isNormalisedWithTrailingSlash() {
        assertEquals("https://x.azurewebsites.net/", ApiFactory.normalizeBaseUrl("https://x.azurewebsites.net"))
        assertEquals("https://x.azurewebsites.net/", ApiFactory.normalizeBaseUrl(" https://x.azurewebsites.net// "))
    }
}
