package com.unitrack.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.unitrack.app.data.api.AuthApiService
import com.unitrack.app.data.network.api.auth.SessionCredentials
import com.unitrack.app.data.network.api.auth.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Base64

class NetworkRegressionTest {
    companion object {
        private fun token(owner: String, revision: Int) = "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"userId":"$owner","revision":$revision}""".toByteArray()) + ".fixture"
        private val OLD_ACCESS = token("student", 1)
        private val NEW_ACCESS = token("student", 2)
    }
    private class Credentials : SessionCredentials {
        @Volatile var access: String? = OLD_ACCESS
        @Volatile var refresh: String? = "old-refresh"
        override fun accessToken() = access
        override fun refreshToken() = refresh
        @Synchronized override fun saveIfCurrent(expectedRefresh: String, access: String, refresh: String): Boolean {
            if (this.refresh != expectedRefresh) return false
            this.access = access; this.refresh = refresh; return true
        }
        @Synchronized override fun clearIfCurrent(expectedRefresh: String?): Boolean {
            if (refresh != expectedRefresh) return false
            access = null; refresh = null; return true
        }
    }

    @Test fun getRetriesButMutationIsSentOnlyOnce() {
        MockWebServer().use { server ->
            val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor(baseDelayMillis = 0)).build()
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setBody("ok"))
            client.newCall(Request.Builder().url(server.url("/courses")).build()).execute().use { assertEquals(200, it.code) }
            assertEquals(2, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(503))
            client.newCall(Request.Builder().url(server.url("/courses")).post("{}".toRequestBody()).build()).execute().use { assertEquals(503, it.code) }
            assertEquals(3, server.requestCount)
        }
    }

    @Test fun transientRefreshFailurePreservesSessionButUnauthorizedClearsIt() {
        MockWebServer().use { server ->
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
                .build().create(AuthApiService::class.java)
            val credentials = Credentials()
            var invalidations = 0
            val auth = TokenAuthenticator(credentials, { api }, { invalidations++ })
            val response = Response.Builder().request(Request.Builder().url(server.url("/courses")).header("Authorization", "Bearer $OLD_ACCESS").build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
            server.enqueue(MockResponse().setResponseCode(503))
            assertNull(auth.authenticate(null, response))
            assertEquals("old-refresh", credentials.refresh)
            assertEquals(0, invalidations)
            server.enqueue(MockResponse().setBody("{}").setHeader("Content-Type", "application/json"))
            assertNull(auth.authenticate(null, response))
            assertEquals("old-refresh", credentials.refresh)
            server.enqueue(MockResponse().setResponseCode(401))
            assertNull(auth.authenticate(null, response))
            assertNull(credentials.refresh)
            assertEquals(1, invalidations)
        }
    }

    @Test fun concurrentUnauthorizedRequestsUseOneRefresh() {
        MockWebServer().use { server ->
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
                .build().create(AuthApiService::class.java)
            val credentials = Credentials()
            val auth = TokenAuthenticator(credentials, { api }, { fail("valid rotation must preserve session") })
            val response = Response.Builder().request(Request.Builder().url(server.url("/courses")).header("Authorization", "Bearer $OLD_ACCESS").build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
            server.enqueue(MockResponse().setBody("""{"accessToken":"$NEW_ACCESS","refreshToken":"new-refresh"}""").setHeader("Content-Type", "application/json"))
            val executor = Executors.newFixedThreadPool(2)
            try {
                val calls = (1..2).map { executor.submit<Request?> { auth.authenticate(null, response) } }
                calls.forEach { assertEquals("Bearer $NEW_ACCESS", it.get()?.header("Authorization")) }
                assertEquals(1, server.requestCount)
                assertEquals("new-refresh", credentials.refresh)
            } finally { executor.shutdownNow() }
        }
    }

    @Test fun delayedRefreshCannotRestoreSignOutOrInvalidateANewerAccount() {
        for (status in listOf(200, 401)) MockWebServer().use { server ->
            val arrived = CountDownLatch(1)
            val resume = CountDownLatch(1)
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    arrived.countDown()
                    check(resume.await(5, TimeUnit.SECONDS))
                    return MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json")
                        .setBody("""{"accessToken":"$NEW_ACCESS","refreshToken":"new-refresh"}""")
                }
            }
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType())).build().create(AuthApiService::class.java)
            val credentials = Credentials()
            val auth = TokenAuthenticator(credentials, { api }, { fail("old refresh must not sign out a newer account") })
            val response = Response.Builder().request(Request.Builder().url(server.url("/courses")).header("Authorization", "Bearer $OLD_ACCESS").build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
            val executor = Executors.newSingleThreadExecutor()
            try {
                val pending = executor.submit<Request?> { auth.authenticate(null, response) }
                assertTrue(arrived.await(5, TimeUnit.SECONDS))
                credentials.clearIfCurrent("old-refresh")
                if (status == 401) { credentials.access = token("another-student", 1); credentials.refresh = "another-refresh" }
                resume.countDown()
                assertNull(pending.get(5, TimeUnit.SECONDS))
                assertEquals(if (status == 401) "another-refresh" else null, credentials.refresh)
            } finally { resume.countDown(); executor.shutdownNow() }
        }
    }

    @Test fun anOldAccountRequestIsNeverRetriedWithAnotherAccountsCredential() {
        val credentials = Credentials().apply { access = token("another-student", 1); refresh = "another-refresh" }
        val auth = TokenAuthenticator(credentials, { error("refresh must not run") }, { fail("new account must remain signed in") })
        val response = Response.Builder().request(Request.Builder().url("https://example.invalid/settings").header("Authorization", "Bearer $OLD_ACCESS").build())
            .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
        assertNull(auth.authenticate(null, response))
        assertEquals("another-refresh", credentials.refresh)
    }

    @Test fun retryLimitCannotInvalidateAnAccountChangedDuringTheCredentialCheck() {
        var refresh: String? = "old-refresh"
        val credentials = object : SessionCredentials {
            override fun refreshToken() = refresh
            override fun accessToken(): String { refresh = "another-refresh"; return OLD_ACCESS }
            override fun saveIfCurrent(expectedRefresh: String, access: String, refresh: String) = false
            override fun clearIfCurrent(expectedRefresh: String?): Boolean {
                if (refresh != expectedRefresh) return false
                refresh = null
                return true
            }
        }
        val auth = TokenAuthenticator(credentials, { error("refresh must not run") }, { fail("another account must remain signed in") })
        val request = Request.Builder().url("https://example.invalid/courses").header("Authorization", "Bearer $OLD_ACCESS").build()
        fun response(prior: Response? = null) = Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").priorResponse(prior).build()
        assertNull(auth.authenticate(null, response(response(response()))))
        assertEquals("another-refresh", refresh)
    }
}
