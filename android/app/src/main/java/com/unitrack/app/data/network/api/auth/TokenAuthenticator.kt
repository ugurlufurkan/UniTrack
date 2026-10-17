package com.unitrack.app.data.network.api.auth

import com.unitrack.app.data.api.AuthApiService
import com.unitrack.app.data.dto.RefreshTokenRequest
import com.unitrack.app.data.repository.AuthPreferences
import com.unitrack.app.data.session.SessionEventBus
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

// Not: Bu sınıf artık SessionManager (sync SharedPreferences) DEĞİL, uygulamanın
// gerçekte kullandığı AuthPreferences (DataStore) üzerinden çalışıyor.
// Authenticator.authenticate() OkHttp tarafından ayrı bir thread'de senkron
// çağrıldığı için, DataStore'un suspend API'lerine runBlocking ile erişiyoruz.
// Bu, ana thread'i BLOKLAMAZ çünkü zaten OkHttp'nin arka plan thread'inde çalışıyoruz.
internal interface SessionCredentials {
    fun accessToken(): String?
    fun refreshToken(): String?
    fun saveIfCurrent(expectedRefresh: String, access: String, refresh: String): Boolean
    fun clearIfCurrent(expectedRefresh: String?): Boolean
}

class TokenAuthenticator internal constructor(
    private val credentials: SessionCredentials,
    private val authApi: () -> AuthApiService,
    private val notifyLogout: () -> Unit
) : Authenticator {
    @Inject constructor(
        authPreferences: AuthPreferences,
        authApiLazy: Lazy<AuthApiService>,
        sessionEventBus: SessionEventBus
    ) : this(object : SessionCredentials {
        override fun accessToken() = runBlocking { authPreferences.getAccessTokenOnce() }
        override fun refreshToken() = runBlocking { authPreferences.getRefreshTokenOnce() }
        override fun saveIfCurrent(expectedRefresh: String, access: String, refresh: String) = runBlocking { authPreferences.saveRotatedTokens(expectedRefresh, access, refresh) }
        override fun clearIfCurrent(expectedRefresh: String?) = runBlocking { authPreferences.clearTokensIfCurrent(expectedRefresh) }
    }, { authApiLazy.get() }, { sessionEventBus.notifyForceLogout() })

    override fun authenticate(route: Route?, response: Response): Request? {
        // Eğer 401 hatası bizzat "refresh" endpoint'inden geliyorsa, sonsuz döngüye girmemek için işlemi kes.
        // NOT: Sadece "/auth/refresh" ile eşleşiyor, "/api/" veya "/api/v1/" önekinden bağımsız —
        // base URL ileride /api/v1/'e taşınırsa bu kontrol sessizce kırılmasın diye.
        if (response.request.url.encodedPath.contains("/auth/refresh")) {
            // The outer refresh call owns the credential it may invalidate.
            return null
        }

        // Aynı istek zaten birkaç kez retry edildiyse (yeni token da 401 alıyorsa) pes et.
        // Böylece bozuk bir refresh döngüsü sonsuza kadar dönmez.
        if (responseCount(response) >= 3) {
            if (credentials.accessToken() == response.request.header("Authorization")?.removePrefix("Bearer "))
                forceLogout(credentials.refreshToken())
            return null
        }

        // Çoklu isteklerde birden fazla kez refresh atılmaması için synchronized kullanıyoruz (Thread safety)
        synchronized(this) {
            val currentToken = credentials.accessToken()
            val incomingToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // Eğer biz beklerken başka bir paralel istek token'ı çoktan yenilemişse, direkt o yeni token'ı kullan.
            if (!currentToken.isNullOrBlank() && currentToken != incomingToken) {
                if (tokenOwner(currentToken) == null || tokenOwner(currentToken) != tokenOwner(incomingToken)) return null
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Refresh token yoksa direkt çıkış yap
            val refreshToken = credentials.refreshToken() ?: run {
                forceLogout(null)
                return null
            }

            // Yeni tokenları Backend'den senkron (execute) olarak alıyoruz
            val refreshResponse = try {
                authApi().refreshToken(RefreshTokenRequest(refreshToken)).execute()
            } catch (e: Exception) {
                // Ağ hatası (internet yok, timeout): bu geçici bir durum, oturumu düşürmeye
                // gerek yok. Sadece bu isteği pes ettir; bağlantı gelince normal akışa döner.
                return null
            }

            return if (refreshResponse.isSuccessful) {
                refreshResponse.body()?.takeIf { it.accessToken.isNotBlank() && it.refreshToken.isNotBlank() }?.let { newTokens ->
                    // Yeni tokenları DataStore'a kaydet (kullanıcı adı korunur, saveTokens null name'i overwrite etmiyor)
                    if (!credentials.saveIfCurrent(refreshToken, newTokens.accessToken, newTokens.refreshToken)) return null

                    // 401 alan o ilk başarısız isteği (örneğin /api/dashboard) yeni token ile KOPYALAYIP tekrar yolla
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.accessToken}")
                        .build()
                }
            } else {
                // Refresh token da expire olmuş, geçersiz, ya da reuse tespit edilmiş
                // (backend tüm oturumları düşürmüş olabilir). Oturumu kapat.
                refreshResponse.errorBody()?.close()
                if (refreshResponse.code() == 401 || refreshResponse.code() == 403) forceLogout(refreshToken)
                null
            }
        }
    }

    /**
     * Token'ları temizler VE bunu SessionEventBus üzerinden UI katmanına bildirir.
     * AuthViewModel bu event'i dinleyip authState'i Idle'a çeker, böylece kullanıcı
     * arka planda düşen bir oturumla bozuk bir ekranda takılı kalmak yerine
     * otomatik olarak LoginScreen'e döner.
     */
    private fun forceLogout(expectedRefresh: String?) {
        if (credentials.clearIfCurrent(expectedRefresh)) notifyLogout()
    }

    private fun tokenOwner(token: String?): String? = runCatching {
        val payload = token?.split('.')?.getOrNull(1) ?: return null
        Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8)).jsonObject["userId"]?.jsonPrimitive?.content
    }.getOrNull()

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
