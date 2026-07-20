package com.unitrack.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.unitrack.app.data.dto.TranscriptEntryDto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Transkript verisinden A4 boyutunda çok sayfalı bir PDF üretir ve
 * FileProvider üzerinden paylaşılabilir bir Uri döndürür.
 *
 * Herhangi bir üçüncü parti kütüphaneye ihtiyaç duymaz; Android'in kendi
 * android.graphics.pdf.PdfDocument API'sini kullanır, bu yüzden internet
 * bağlantısı olmadan (OBS'den bağımsız) da çalışır.
 */
object TranscriptPdfExporter {

    // A4, 72 dpi (nokta) cinsinden
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val ROW_HEIGHT = 24f
    private const val HEADER_HEIGHT = 120f

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 20f
        isFakeBoldText = true
    }

    private val subtitlePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 11f
    }

    private val tableHeaderPaint = Paint().apply {
        color = Color.WHITE
        textSize = 11f
        isFakeBoldText = true
    }

    private val tableHeaderBgPaint = Paint().apply {
        color = Color.parseColor("#1F5AA6")
        style = Paint.Style.FILL
    }

    private val rowPaint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
    }

    private val rowAltBgPaint = Paint().apply {
        color = Color.parseColor("#F2F4F7")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#D0D5DD")
        strokeWidth = 1f
    }

    private val gpaPaint = Paint().apply {
        color = Color.parseColor("#1F5AA6")
        textSize = 16f
        isFakeBoldText = true
    }

    // Kolon başlıkları ve göreli genişlikleri (toplam = 1.0)
    private val columns = listOf(
        "Ders" to 0.40f,
        "Kredi" to 0.12f,
        "Ortalama" to 0.18f,
        "Harf Notu" to 0.15f,
        "Katsayı" to 0.15f
    )

    fun export(
        context: Context,
        studentName: String,
        gpa: Double,
        entries: List<TranscriptEntryDto>
    ): Uri {
        val document = PdfDocument()
        val tableWidth = PAGE_WIDTH - 2 * MARGIN

        var page = newPage(document, pageNumber = 1)
        var canvas = page.canvas
        var y = drawHeader(canvas, studentName, gpa, entries.size)
        y = drawTableHeader(canvas, y, tableWidth)

        var pageNumber = 1
        entries.forEachIndexed { index, entry ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = newPage(document, pageNumber)
                canvas = page.canvas
                y = MARGIN
                y = drawTableHeader(canvas, y, tableWidth)
            }

            drawRow(canvas, y, tableWidth, index, entry)
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        val outDir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val fileName = "transkript_${System.currentTimeMillis()}.pdf"
        val file = File(outDir, fileName)

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /** PDF'i paylaşmak (WhatsApp, Drive, e-posta vb.) için hazır bir Intent döndürür. */
    fun buildShareIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun newPage(document: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return document.startPage(info)
    }

    private fun drawHeader(
        canvas: Canvas,
        studentName: String,
        gpa: Double,
        courseCount: Int
    ): Float {
        var y = MARGIN + 20f
        canvas.drawText("UniTrack – Akademik Transkript", MARGIN, y, titlePaint)

        y += 20f
        canvas.drawText("Öğrenci: $studentName", MARGIN, y, subtitlePaint)

        y += 16f
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(java.util.Date())
        canvas.drawText("Oluşturulma tarihi: $dateStr   •   Toplam ders: $courseCount", MARGIN, y, subtitlePaint)

        y += 26f
        canvas.drawText("Genel Not Ortalaması (GANO): ${String.format(Locale("tr", "TR"), "%.2f", gpa)}", MARGIN, y, gpaPaint)

        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)

        return y + 16f
    }

    private fun drawTableHeader(canvas: Canvas, startY: Float, tableWidth: Float): Float {
        canvas.drawRect(MARGIN, startY, MARGIN + tableWidth, startY + ROW_HEIGHT, tableHeaderBgPaint)

        var x = MARGIN + 6f
        columns.forEach { (label, weight) ->
            canvas.drawText(label, x, startY + ROW_HEIGHT - 7f, tableHeaderPaint)
            x += tableWidth * weight
        }

        return startY + ROW_HEIGHT
    }

    private fun drawRow(
        canvas: Canvas,
        startY: Float,
        tableWidth: Float,
        index: Int,
        entry: TranscriptEntryDto
    ) {
        if (index % 2 == 1) {
            canvas.drawRect(MARGIN, startY, MARGIN + tableWidth, startY + ROW_HEIGHT, rowAltBgPaint)
        }

        val averageText = entry.average?.let { String.format(Locale("tr", "TR"), "%.1f", it) } ?: "-"
        val letterText = entry.letter.ifBlank { "-" }
        val pointText = entry.point?.let { String.format(Locale("tr", "TR"), "%.1f", it) } ?: "-"

        val values = listOf(
            truncate(entry.course, 28),
            entry.credit.toString(),
            averageText,
            letterText,
            pointText
        )

        var x = MARGIN + 6f
        values.forEachIndexed { i, value ->
            canvas.drawText(value, x, startY + ROW_HEIGHT - 7f, rowPaint)
            x += tableWidth * columns[i].second
        }

        canvas.drawLine(MARGIN, startY + ROW_HEIGHT, MARGIN + tableWidth, startY + ROW_HEIGHT, linePaint)
    }

    private fun truncate(text: String, max: Int): String {
        return if (text.length > max) text.take(max - 1) + "…" else text
    }
}
