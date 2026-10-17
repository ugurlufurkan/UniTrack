package com.unitrack.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.unitrack.app.data.api.SettingsApiService
import com.unitrack.app.data.dto.SettingsPatches
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit

class SettingsSyncTest {
    @Test fun changingOnePreferenceNeverSendsUnrelatedDefaultsOrNulls() = runBlocking {
        MockWebServer().use { server ->
            val json = Json { encodeDefaults = true }
            val api = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(SettingsApiService::class.java)
            for (patch in listOf(SettingsPatches.theme("DARK"), SettingsPatches.target(3.2), SettingsPatches.clearExamPeriod())) {
                server.enqueue(MockResponse().setBody("{}").setHeader("Content-Type", "application/json"))
                assertTrue(api.updateSettings(patch).isSuccessful)
                val sent = json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
                assertEquals(patch, sent)
                if ("themePreference" in sent) assertEquals(setOf("themePreference"), sent.keys)
                if ("targetGpa" in sent) assertEquals(setOf("targetGpa"), sent.keys)
                if ("examPeriodStart" in sent) {
                    assertEquals(setOf("examPeriodStart", "examPeriodEnd"), sent.keys)
                    assertEquals(JsonNull, sent["examPeriodStart"])
                    assertEquals(JsonNull, sent["examPeriodEnd"])
                }
            }
        }
    }
}
