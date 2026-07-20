package com.unitrack.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore veritabanının adını tanımlıyoruz
private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Kaydedeceğimiz anahtarların isimleri
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_NAME = stringPreferencesKey("user_name")
    }

    // İstediğimiz zaman token'ları okumak için
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }

    // Anlık (suspend olmayan) okuma gerektiğinde, ör. OkHttp interceptor içinde
    suspend fun getAccessTokenOnce(): String? = accessToken.first()

    // TokenAuthenticator refresh akışında ihtiyaç duyulan anlık refresh token okuması
    suspend fun getRefreshTokenOnce(): String? = refreshToken.first()

    // Backend'den gelen yeni token'ları telefona kaydetmek için
    suspend fun saveTokens(access: String, refresh: String, name: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = access
            prefs[REFRESH_TOKEN] = refresh
            if (name != null) {
                prefs[USER_NAME] = name
            }
        }
    }

    // Refresh sonrası sadece access token güncellenir
    suspend fun updateAccessToken(access: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = access
        }
    }

    // Çıkış yapıldığında token'ları silmek için
    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}