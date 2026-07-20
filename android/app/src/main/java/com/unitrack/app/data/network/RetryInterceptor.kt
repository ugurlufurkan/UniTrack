package com.unitrack.app.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Geçici ağ hatalarında (bağlantı koptu, timeout, sunucu geçici olarak 5xx
 * döndü) isteği kısa bir bekleme ile otomatik olarak tekrar dener. Böylece
 * kullanıcı, zayıf bir internet anında gereksiz yere hata ekranına düşmez.
 *
 * Yalnızca GET istekleri tekrar denenir. POST/PUT/PATCH/DELETE gibi yan
 * etkisi olan isteklerde, ağ hatası isteğin sunucuya ulaşıp ulaşmadığını
 * belirsiz bırakır — bunları körü körüne tekrar göndermek (ör. aynı dersin
 * iki kez oluşturulması) tekrar denemekten daha tehlikelidir.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val baseDelayMillis: Long = 500L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.method != "GET") {
            return chain.proceed(request)
        }

        var lastException: IOException? = null

        for (attempt in 0..maxRetries) {
            if (attempt > 0) {
                // 500ms, 1000ms, 2000ms... üstel geri çekilme.
                Thread.sleep(baseDelayMillis * (1L shl (attempt - 1)))
            }

            try {
                val response = chain.proceed(request)

                val isRetryableServerError = response.code >= 500
                if (!isRetryableServerError || attempt == maxRetries) {
                    return response
                }

                // Tekrar denenecek: bu cevabı sızdırmadan kapat, bir sonraki
                // döngüde yeni bir istek atılacak.
                response.close()
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) {
                    throw e
                }
            }
        }

        // Buraya normalde hiç düşülmez (döngü içinde ya return ya throw olur),
        // ama derleyiciyi tatmin etmek ve savunma amaçlı son bir fallback.
        throw lastException ?: IOException("İstek tekrar denemelerine rağmen tamamlanamadı.")
    }
}
