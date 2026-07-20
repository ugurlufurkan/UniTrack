package com.unitrack.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.local.ExamPeriodPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * MainActivity'nin en kök seviyesinde (AmbientBackground'ı sarmalayan yer)
 * kullanılır — "bugün kullanıcının kendi belirlediği sınav haftası içinde
 * mi?" sorusunun tek cevabı burada üretilir, AmbientBackground'ın kendisi
 * DataStore/Hilt'ten habersiz kalır (bkz. AmbientBackground.kt'deki
 * `examWeek: Boolean` parametresi).
 */
@HiltViewModel
class AmbientMoodViewModel @Inject constructor(
    examPeriodPreferences: ExamPeriodPreferences
) : ViewModel() {

    val isExamWeek: StateFlow<Boolean> = examPeriodPreferences.examPeriod
        .map { period -> period?.contains(LocalDate.now()) == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
}
