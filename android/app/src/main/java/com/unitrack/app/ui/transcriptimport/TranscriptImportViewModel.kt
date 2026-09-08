package com.unitrack.app.ui.transcriptimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.ImportTranscriptCourseRequest
import com.unitrack.app.data.dto.ImportTranscriptRequest
import com.unitrack.app.data.dto.ImportTranscriptSemesterRequest
import com.unitrack.app.data.dto.ParsedTranscriptSemesterDto
import com.unitrack.app.data.repository.TranscriptImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Önizlemedeki her dersin yanında bir "dahil et" kutucuğu olacağı için
// backend DTO'sunu (salt okunur) sarmalayan, ekranda tutulan bir kopya.
data class PreviewCourseItem(
    val name: String,
    val credit: Int,
    val grade: String,
    val recognized: Boolean,
    val included: Boolean = true
)

data class PreviewSemesterItem(
    val year: Int,
    val term: String,
    val academicYearLabel: String,
    val courses: List<PreviewCourseItem>
)

sealed class TranscriptImportUiState {
    object Idle : TranscriptImportUiState()
    object Parsing : TranscriptImportUiState()
    data class Preview(val semesters: List<PreviewSemesterItem>) : TranscriptImportUiState()
    object Importing : TranscriptImportUiState()
    data class Success(val importedCourses: Int) : TranscriptImportUiState()
    data class Error(val message: String) : TranscriptImportUiState()
}

@HiltViewModel
class TranscriptImportViewModel @Inject constructor(
    private val repository: TranscriptImportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranscriptImportUiState>(TranscriptImportUiState.Idle)
    val uiState: StateFlow<TranscriptImportUiState> = _uiState.asStateFlow()

    fun onFilePicked(uri: Uri) {
        _uiState.value = TranscriptImportUiState.Parsing
        viewModelScope.launch {
            try {
                val parsed: List<ParsedTranscriptSemesterDto> = repository.parse(uri)

                if (parsed.isEmpty() || parsed.all { it.courses.isEmpty() }) {
                    _uiState.value = TranscriptImportUiState.Error(
                        "PDF içinde okunabilir bir ders/dönem bulunamadı. Bu dosya farklı bir üniversitenin formatında olabilir."
                    )
                    return@launch
                }

                _uiState.value = TranscriptImportUiState.Preview(
                    parsed.map { sem ->
                        PreviewSemesterItem(
                            year = sem.year,
                            term = sem.term,
                            academicYearLabel = sem.academicYearLabel,
                            courses = sem.courses.map {
                                PreviewCourseItem(
                                    name = it.name,
                                    credit = it.credit,
                                    grade = it.grade,
                                    recognized = it.recognized
                                )
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = TranscriptImportUiState.Error(
                    e.message ?: "PDF okunurken bir hata oluştu."
                )
            }
        }
    }

    /** Bir dersin dahil/hariç durumunu değiştirir (önizleme ekranındaki checkbox). */
    fun toggleCourse(semesterIndex: Int, courseIndex: Int) {
        val current = _uiState.value
        if (current !is TranscriptImportUiState.Preview) return

        val updatedSemesters = current.semesters.mapIndexed { sIdx, sem ->
            if (sIdx != semesterIndex) return@mapIndexed sem
            sem.copy(
                courses = sem.courses.mapIndexed { cIdx, course ->
                    if (cIdx != courseIndex) course else course.copy(included = !course.included)
                }
            )
        }
        _uiState.value = current.copy(semesters = updatedSemesters)
    }

    /** Bir dönemin tüm derslerini tek seferde dahil/hariç bırakır. */
    fun toggleSemester(semesterIndex: Int, included: Boolean) {
        val current = _uiState.value
        if (current !is TranscriptImportUiState.Preview) return

        val updatedSemesters = current.semesters.mapIndexed { sIdx, sem ->
            if (sIdx != semesterIndex) return@mapIndexed sem
            sem.copy(courses = sem.courses.map { it.copy(included = included) })
        }
        _uiState.value = current.copy(semesters = updatedSemesters)
    }

    fun confirmImport() {
        val current = _uiState.value
        if (current !is TranscriptImportUiState.Preview) return

        val payload = current.semesters
            .map { sem -> sem.copy(courses = sem.courses.filter { it.included }) }
            .filter { it.courses.isNotEmpty() }

        if (payload.isEmpty()) {
            _uiState.value = TranscriptImportUiState.Error(
                "Aktarılacak en az bir ders seçmelisin."
            )
            return
        }

        _uiState.value = TranscriptImportUiState.Importing
        viewModelScope.launch {
            try {
                val request = ImportTranscriptRequest(
                    semesters = payload.map { sem ->
                        ImportTranscriptSemesterRequest(
                            year = sem.year,
                            term = sem.term,
                            courses = sem.courses.map {
                                ImportTranscriptCourseRequest(
                                    name = it.name,
                                    credit = it.credit,
                                    grade = it.grade
                                )
                            }
                        )
                    }
                )

                val result = repository.import(request)
                _uiState.value = TranscriptImportUiState.Success(result.importedCourses)
            } catch (e: Exception) {
                _uiState.value = TranscriptImportUiState.Error(
                    e.message ?: "Aktarım sırasında bir hata oluştu."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = TranscriptImportUiState.Idle
    }
}
