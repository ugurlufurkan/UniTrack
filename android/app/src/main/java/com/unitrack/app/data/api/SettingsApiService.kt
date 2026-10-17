package com.unitrack.app.data.api

import com.unitrack.app.data.dto.SettingsDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface SettingsApiService {

    @GET("settings")
    suspend fun getSettings(): Response<SettingsDto>

    // Only explicitly changed fields are included; explicit JSON null clears a setting.
    @PATCH("settings")
    suspend fun updateSettings(@Body request: JsonObject): Response<SettingsDto>
}
