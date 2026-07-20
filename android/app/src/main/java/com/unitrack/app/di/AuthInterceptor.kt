package com.unitrack.app.di

import com.unitrack.app.data.repository.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking {
            authPreferences.getAccessTokenOnce()
        }

        val request = chain.request().newBuilder().apply {
            if (!accessToken.isNullOrBlank()) {
                header("Authorization", "Bearer $accessToken")
            }

            header("Accept", "application/json")
            header("Content-Type", "application/json")
        }.build()

        // 401 handling (refresh + retry, ya da kurtarılamazsa oturumu kapatma)
        // artık tamamen TokenAuthenticator'ın sorumluluğunda — Authenticator, bu
        // interceptor'ın DAHA İÇİNDE çalışır, yani buraya bir 401 sızdıysa
        // TokenAuthenticator zaten denemiş ve vazgeçmiş demektir. Burada tekrar
        // token temizlemek gereksiz/kafa karıştırıcıydı, kaldırıldı.
        return chain.proceed(request)
    }
}