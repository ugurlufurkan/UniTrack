package com.unitrack.app.data.api

import com.unitrack.app.data.dto.SettingsDto
import com.unitrack.app.data.dto.SettingsUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface SettingsApiService {

    @GET("settings")
    suspend fun getSettings(): Response<SettingsDto>

    // NOT: Json { encodeDefaults = true } açık olduğu için istek gövdesinde
    // TÜM alanlar (dahil edilmeyenler null olarak) gönderilir. Bu yüzden
    // çağıran taraf (SettingsSyncRepository) her zaman güncel/tam bir
    // SettingsUpdateRequest oluşturup göndermeli — aksi halde tek bir alanı
    // değiştirmek diğerlerini sunucuda null'a sıfırlar.
    @PATCH("settings")
    suspend fun updateSettings(@Body request: SettingsUpdateRequest): Response<SettingsDto>
}
