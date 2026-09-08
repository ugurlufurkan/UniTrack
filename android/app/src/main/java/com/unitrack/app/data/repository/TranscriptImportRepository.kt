package com.unitrack.app.data.repository

import android.content.Context
import android.net.Uri
import com.unitrack.app.data.api.TranscriptImportApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.dto.ImportTranscriptRequest
import com.unitrack.app.data.dto.ImportTranscriptResponse
import com.unitrack.app.data.dto.ParsedTranscriptSemesterDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptImportRepository @Inject constructor(
    private val api: TranscriptImportApiService,
    private val json: Json,
    @ApplicationContext private val context: Context
) {
    /** Kullanıcının Dosyalar/Drive vb. üzerinden seçtiği PDF'in Uri'sini okuyup yükler. */
    suspend fun parse(uri: Uri): List<ParsedTranscriptSemesterDto> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Dosya okunamadı.")

        val requestBody = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "transkript.pdf", requestBody)

        api.parseTranscript(part).bodyOrThrow(json).semesters
    }

    suspend fun import(request: ImportTranscriptRequest): ImportTranscriptResponse =
        api.importTranscript(request).bodyOrThrow(json)
}
