package com.unitrack.app.ui.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.SemesterDto
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SemesterViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _semesters =
        MutableStateFlow<List<SemesterDto>>(emptyList())

    val semesters: StateFlow<List<SemesterDto>> =
        _semesters.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

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

                _errorMessage.value =
                    it.message ?: "Dönemler yüklenemedi."

            }

            _isLoading.value = false

        }

    }

    fun addSemester(
        year: Int,
        term: String
    ) {

        viewModelScope.launch {

            runCatching {

                repository.createSemester(
                    year,
                    term
                )

            }.onSuccess {

                refresh()

            }.onFailure {

                _errorMessage.value =
                    it.message ?: "Dönem eklenemedi."

            }

        }

    }

    fun deleteSemester(
        id: String
    ) {

        viewModelScope.launch {

            runCatching {

                repository.deleteSemester(id)

            }.onSuccess {

                refresh()

            }.onFailure {

                _errorMessage.value =
                    it.message ?: "Dönem silinemedi."

            }

        }

    }

    fun clearError() {
        _errorMessage.value = null
    }

}