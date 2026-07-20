package com.unitrack.app.data.api

import com.unitrack.app.data.dto.ErrorResponseDto
import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * Başarılıysa gövdeyi döner. Değilse, backend'in error middleware'inin
 * yolladığı { success:false, message:"..." } gövdesini okuyup
 * anlamlı bir hata olarak fırlatır.
 */
suspend fun <T> Response<T>.bodyOrThrow(json: Json): T {
    if (isSuccessful) {
        return body() ?: throw IllegalStateException("Sunucudan boş cevap geldi.")
    }

    val raw = errorBody()?.string()
    val parsed = raw?.let {
        try {
            json.decodeFromString(ErrorResponseDto.serializer(), it)
        } catch (e: Exception) {
            null
        }
    }

    // Backend, Zod doğrulama hatalarında sadece genel "Validation Error" mesajı
    // gönderiyor; asıl neyin geçersiz olduğu `errors` dizisinde. Onu da ekleyip
    // kullanıcıya anlamlı bir hata gösteriyoruz — önceden burada sadece
    // "Validation Error" görünüyordu, hiçbir ayrıntı yoktu.
    val detail = parsed?.errors
        ?.mapNotNull { it.message.takeIf { m -> m.isNotBlank() } }
        ?.distinct()
        ?.joinToString("\n")

    val message = when {
        !detail.isNullOrBlank() -> detail
        !parsed?.message.isNullOrBlank() -> parsed?.message
        else -> "Sunucu hatası (${code()})"
    }

    throw IllegalStateException(message)
}

/** Body beklenmeyen (204/boş) isteklerde sadece başarı kontrolü yapar. */
suspend fun <T> Response<T>.requireSuccess(json: Json) {
    if (!isSuccessful) {
        bodyOrThrow(json)
    }
}