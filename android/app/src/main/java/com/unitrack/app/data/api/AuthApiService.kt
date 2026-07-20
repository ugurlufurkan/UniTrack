package com.unitrack.app.data.api

import com.unitrack.app.data.dto.AuthResponse
import com.unitrack.app.data.dto.GoogleLoginRequest
import com.unitrack.app.data.dto.LogoutRequest
import com.unitrack.app.data.dto.RefreshTokenRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {

    // Adresler backend rotaları ile değiştirildi
    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    // Bu zaten doğruydu
    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<AuthResponse>

    // Google Play "hesap silme" şartı için: hesabı ve ilişkili verileri
    // kalıcı olarak siler. Backend'de bu rota HENÜZ YOKSA eklenmesi
    // gerekiyor — beklenen davranış: geçerli access token'daki kullanıcıyı
    // (courses/semesters/attendance/tasks/events/refresh token'lar dahil)
    // kalıcı olarak siler ve 204 döner. Var olmayan bir kullanıcı/geçersiz
    // token için 401/404 döndürülmesi yeterli, istemci zaten bu durumları
    // "başarısız" sayıp kullanıcıya hata gösteriyor.
    @DELETE("auth/me")
    suspend fun deleteAccount(): Response<Unit>
}
