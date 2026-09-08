package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// TRANSCRIPT IMPORT (POST /api/transcript-import/parse ve /import)
// ==========================

@Serializable
data class ParsedTranscriptCourseDto(
    val name: String = "",
    val credit: Int = 0,
    val grade: String = "",
    // false ise: harf notu (AA-FF) skalasında değil (S, MU, DZ, ÇK, GR gibi
    // işaretler) — bu ders puansız (yalnızca isim+kredi) aktarılacak.
    val recognized: Boolean = true
)

@Serializable
data class ParsedTranscriptSemesterDto(
    val year: Int = 0,
    val term: String = "",
    val academicYearLabel: String = "",
    val courses: List<ParsedTranscriptCourseDto> = emptyList()
)

@Serializable
data class ParseTranscriptResponse(
    val semesters: List<ParsedTranscriptSemesterDto> = emptyList()
)

@Serializable
data class ImportTranscriptCourseRequest(
    val name: String,
    val credit: Int,
    val grade: String
)

@Serializable
data class ImportTranscriptSemesterRequest(
    val year: Int,
    val term: String,
    val courses: List<ImportTranscriptCourseRequest>
)

@Serializable
data class ImportTranscriptRequest(
    val semesters: List<ImportTranscriptSemesterRequest>
)

@Serializable
data class ImportTranscriptResponse(
    val importedSemesters: Int = 0,
    val importedCourses: Int = 0
)
