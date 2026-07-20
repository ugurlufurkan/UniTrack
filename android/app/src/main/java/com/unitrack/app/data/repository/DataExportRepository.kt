package com.unitrack.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.unitrack.app.data.api.ExportApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Verilerimi dışa aktar" özelliği: backend'den TÜM kullanıcı verisini tek
 * bir JSON dosyası olarak indirir ve paylaşılabilir bir Uri döner.
 *
 * TranscriptPdfExporter ile aynı desen (cacheDir -> FileProvider -> share
 * Intent) ama burada içerik zaten sunucudan hazır JSON olarak geldiği için
 * bir şey üretmiyoruz, sadece akışı diske döküyoruz.
 */
@Singleton
class DataExportRepository @Inject constructor(
    private val api: ExportApiService,
    @ApplicationContext private val context: Context
) {
    /** @return indirilen dosyanın paylaşılabilir Uri'si. */
    suspend fun downloadBackup(): Uri = withContext(Dispatchers.IO) {
        val response = api.exportMyData()
        val body = response.body()

        if (!response.isSuccessful || body == null) {
            throw IllegalStateException("Yedek indirilemedi (kod ${response.code()}).")
        }

        val outDir = File(context.cacheDir, "backup").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale("tr", "TR")).format(java.util.Date())
        val file = File(outDir, "unitrack-yedek-$stamp.json")

        body.byteStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
