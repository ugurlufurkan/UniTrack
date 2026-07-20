package com.unitrack.app.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.CourseComponentDto
import com.unitrack.app.data.dto.CourseDto
import com.unitrack.app.data.dto.CreateCourseRequest
import com.unitrack.app.data.dto.GradeBandDto
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.data.dto.UpdateCourseRequest
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _courses = MutableStateFlow<List<CourseDto>>(emptyList())
    val courses: StateFlow<List<CourseDto>> = _courses.asStateFlow()

    private val _semesters = MutableStateFlow<List<SemesterDto>>(emptyList())
    val semesters: StateFlow<List<SemesterDto>> = _semesters.asStateFlow()

    // Kullanıcının genel varsayılan harf notu skalası (ders bazında override edilmezse kullanılır).
    private val _defaultGradeScale = MutableStateFlow<List<GradeBandDto>>(emptyList())
    val defaultGradeScale: StateFlow<List<GradeBandDto>> = _defaultGradeScale.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {

            _isLoading.value = true

            runCatching {
                repository.getSemesters()
            }.onSuccess {
                _semesters.value = it
            }.onFailure {
                _errorMessage.value = it.message
            }

            runCatching {
                repository.getCourses()
            }.onSuccess {
                _courses.value = it
            }.onFailure {
                _errorMessage.value = it.message
            }

            runCatching {
                repository.getDefaultGradeScale()
            }.onSuccess {
                _defaultGradeScale.value = it.gradeScale
            }.onFailure {
                _errorMessage.value = it.message
            }

            _isLoading.value = false
        }
    }

    fun addCourse(
        semesterId: String,
        name: String,
        credit: Int,
        components: List<CourseComponentDto>,
        gradeScale: List<GradeBandDto>?,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {

            runCatching {
                repository.createCourse(
                    CreateCourseRequest(
                        semesterId = semesterId,
                        name = name,
                        credit = credit,
                        components = components,
                        gradeScale = gradeScale
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message
                onResult(false)
            }
        }
    }

    fun updateCourse(
        id: String,
        name: String,
        credit: Int,
        components: List<CourseComponentDto>,
        gradeScale: List<GradeBandDto>?,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {

            runCatching {
                repository.updateCourse(
                    id,
                    UpdateCourseRequest(
                        name = name,
                        credit = credit,
                        components = components,
                        gradeScale = gradeScale
                    )
                )
            }.onSuccess {
                refresh()
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message
                onResult(false)
            }
        }
    }

    fun setDefaultGradeScale(gradeScale: List<GradeBandDto>?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                repository.setDefaultGradeScale(gradeScale)
            }.onSuccess {
                _defaultGradeScale.value = it.gradeScale
                onResult(true)
            }.onFailure {
                _errorMessage.value = it.message
                onResult(false)
            }
        }
    }

    fun deleteCourse(id: String) {
        viewModelScope.launch {

            runCatching {
                repository.deleteCourse(id)
            }.onSuccess {
                refresh()
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}