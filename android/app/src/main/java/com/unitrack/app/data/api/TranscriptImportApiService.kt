package com.unitrack.app.data.api

import com.unitrack.app.data.dto.ImportTranscriptRequest
import com.unitrack.app.data.dto.ImportTranscriptResponse
import com.unitrack.app.data.dto.ParseTranscriptResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranscriptImportApiService {

    // Adım 1: PDF'i yükle, KAYDETMEDEN önizleme al.
    @Multipart
    @POST("transcript-import/parse")
    suspend fun parseTranscript(
        @Part file: MultipartBody.Part
    ): Response<ParseTranscriptResponse>

    // Adım 2: Kullanıcının onayladığı (gerekirse düzenlediği) veriyi kaydet.
    @POST("transcript-import/import")
    suspend fun importTranscript(
        @Body request: ImportTranscriptRequest
    ): Response<ImportTranscriptResponse>
}
