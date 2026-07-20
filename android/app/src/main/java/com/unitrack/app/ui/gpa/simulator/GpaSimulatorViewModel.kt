package com.unitrack.app.ui.gpa.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unitrack.app.data.dto.GradeBandDto
import com.unitrack.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.round

/**
 * Sisteme hiç kaydedilmeyen, sadece bu ekranda "ya X dersinden Y alırsam GANO'm
 * ne olur?" sorusunu yanıtlamak için bellekte tutulan hayali ders. Backend'e
 * hiç istek atılmıyor — simülatör tamamen mevcut GPA verisi (gerçek dersler)
 * üzerine client-side hesap yapıyor.
 */
data class SimulatedCourse(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val credit: Int,
    val letter: String,
    val point: Double
)

sealed class GpaSimulatorUiState {
    object Loading : GpaSimulatorUiState()
    data class Error(val message: String) : GpaSimulatorUiState()
    data class Success(
        val currentGpa: Double,
        val currentCredit: Int,
        val currentPoint: Double,
        val gradeScale: List<GradeBandDto>
    ) : GpaSimulatorUiState()
}

/**
 * backend/src/shared/utils/grade.util.ts -> DEFAULT_GRADE_SCALE ile birebir
 * aynı: kullanıcının özel bir varsayılan skalası yoksa (ya da skala ağdan
 * çekilemezse) bu kullanılır. İki taraf birbirinden sapmasın diye burada da
 * elle senkron tutuluyor.
 */
val DEFAULT_GRADE_SCALE = listOf(
    GradeBandDto(letter = "AA", min = 90.0, point = 4.0),
    GradeBandDto(letter = "BA", min = 85.0, point = 3.5),
    GradeBandDto(letter = "BB", min = 80.0, point = 3.0),
    GradeBandDto(letter = "CB", min = 75.0, point = 2.5),
    GradeBandDto(letter = "CC", min = 70.0, point = 2.0),
    GradeBandDto(letter = "DC", min = 65.0, point = 1.5),
    GradeBandDto(letter = "DD", min = 60.0, point = 1.0),
    GradeBandDto(letter = "FD", min = 50.0, point = 0.5),
    GradeBandDto(letter = "FF", min = 0.0, point = 0.0)
)

@HiltViewModel
class GpaSimulatorViewModel @Inject constructor(
    private val repository: AcademicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GpaSimulatorUiState>(GpaSimulatorUiState.Loading)
    val uiState: StateFlow<GpaSimulatorUiState> = _uiState.asStateFlow()

    private val _simulatedCourses = MutableStateFlow<List<SimulatedCourse>>(emptyList())
    val simulatedCourses: StateFlow<List<SimulatedCourse>> = _simulatedCourses.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = GpaSimulatorUiState.Loading

            runCatching {
                val gpa = repository.getGpa()
                // Skala isteğe bağlı: çekilemezse sistem varsayılanına düşüyoruz,
                // bu yüzden ayrı bir runCatching ile bütün ekranı hataya düşürmüyoruz.
                val scale = runCatching { repository.getDefaultGradeScale() }.getOrNull()
                gpa to scale
            }.onSuccess { (gpa, scaleResult) ->
                val completed = gpa.courses.filter { it.completed }
                val credit = completed.sumOf { it.credit }
                val point = completed.sumOf { it.credit * (it.point ?: 0.0) }
                val scale = scaleResult?.gradeScale
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_GRADE_SCALE

                _uiState.value = GpaSimulatorUiState.Success(
                    currentGpa = gpa.gpa,
                    currentCredit = credit,
                    currentPoint = point,
                    gradeScale = scale.sortedByDescending { it.point }
                )
            }.onFailure {
                _uiState.value = GpaSimulatorUiState.Error(it.message ?: "Veriler yüklenemedi.")
            }
        }
    }

    fun addCourse(name: String, credit: Int, letter: String, point: Double) {
        _simulatedCourses.value = _simulatedCourses.value + SimulatedCourse(
            name = name.ifBlank { "Ders" },
            credit = credit,
            letter = letter,
            point = point
        )
    }

    fun removeCourse(id: String) {
        _simulatedCourses.value = _simulatedCourses.value.filterNot { it.id == id }
    }

    fun clearAll() {
        _simulatedCourses.value = emptyList()
    }

    /** Gerçek (tamamlanmış) dersler + eklenen hayali derslerle projekte edilen GANO. */
    fun projectedGpa(state: GpaSimulatorUiState.Success): Double {
        val simCredit = _simulatedCourses.value.sumOf { it.credit }
        val simPoint = _simulatedCourses.value.sumOf { it.credit * it.point }
        val totalCredit = state.currentCredit + simCredit

        if (totalCredit == 0) return 0.0

        val raw = (state.currentPoint + simPoint) / totalCredit
        return round(raw * 100) / 100.0
    }

    /**
     * Hedef GANO hesaplayıcı: [targetGpa]'ya ulaşmak için, planlanan
     * [plannedCredit] kredilik derslerden (henüz eklenmiş hayali dersler
     * HARİÇ, sıfırdan bir "ne almalıyım" sorusu) ortalama kaç puan (4'lük
     * sistemde) alınması gerektiğini döner. [plannedCredit] <= 0 ise null.
     */
    fun requiredPointForTarget(
        state: GpaSimulatorUiState.Success,
        targetGpa: Double,
        plannedCredit: Int
    ): Double? {
        if (plannedCredit <= 0) return null
        val totalCredit = state.currentCredit + plannedCredit
        val raw = (targetGpa * totalCredit - state.currentPoint) / plannedCredit
        return round(raw * 100) / 100.0
    }
}