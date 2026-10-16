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
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.Executors

class NetworkRegressionTest {
    private class Credentials : SessionCredentials {
        var access: String? = "old-access"
        var refresh: String? = "old-refresh"
        override fun accessToken() = access
        override fun refreshToken() = refresh
        override fun save(access: String, refresh: String) { this.access = access; this.refresh = refresh }
        override fun clear() { access = null; refresh = null }
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
            val response = Response.Builder().request(Request.Builder().url(server.url("/courses")).header("Authorization", "Bearer old-access").build())
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
            val response = Response.Builder().request(Request.Builder().url(server.url("/courses")).header("Authorization", "Bearer old-access").build())
                .protocol(Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
            server.enqueue(MockResponse().setBody("""{"accessToken":"new-access","refreshToken":"new-refresh"}""").setHeader("Content-Type", "application/json"))
            val executor = Executors.newFixedThreadPool(2)
            try {
                val calls = (1..2).map { executor.submit<Request?> { auth.authenticate(null, response) } }
                calls.forEach { assertEquals("Bearer new-access", it.get()?.header("Authorization")) }
                assertEquals(1, server.requestCount)
                assertEquals("new-refresh", credentials.refresh)
            } finally { executor.shutdownNow() }
        }
    }
}
