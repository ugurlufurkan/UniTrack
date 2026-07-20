package com.unitrack.app.data.repository

// Android Studio'nun bulamadığı bağlantıları elimizle koyduk:
import com.unitrack.app.data.api.AuthApiService
import com.unitrack.app.data.dto.AuthResponse
import com.unitrack.app.data.dto.GoogleLoginRequest
import com.unitrack.app.data.dto.LogoutRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val authPreferences: AuthPreferences
) {
    suspend fun loginWithGoogle(idToken: String): AuthResponse? {
        val response = apiService.loginWithGoogle(GoogleLoginRequest(idToken))
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun getCurrentUser(): AuthResponse? {
        val response = apiService.getCurrentUser()
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun logout(refreshToken: String) {
        if (refreshToken.isNotBlank()) {
            apiService.logout(LogoutRequest(refreshToken))
        }
    }

    /**
     * Google Play "hesap silme" şartı: hesabı ve backend'deki ilişkili
     * verileri kalıcı olarak siler, başarılıysa yerel oturumu (token'lar)
     * da temizler. Backend'de karşılığı DELETE /auth/me henüz yoksa
     * eklenmesi gerekiyor (bkz. AuthApiService.deleteAccount yorumu).
     */
    suspend fun deleteAccount(): Boolean {
        val response = apiService.deleteAccount()
        if (response.isSuccessful) {
            authPreferences.clearTokens()
            return true
        }
        return false
    }
}
