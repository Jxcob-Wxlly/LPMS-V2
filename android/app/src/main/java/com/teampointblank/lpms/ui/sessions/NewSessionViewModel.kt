package com.teampointblank.lpms.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.CreateSessionRequest
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.Programme
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class NewSessionUiState(
    val programmes: List<Programme> = emptyList(),
    val programme: Programme? = null,
    val date: LocalDate? = null,
    val start: LocalTime? = null,
    val end: LocalTime? = null,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val programmeError: String? = null,
    val dateError: String? = null,
    val timeError: String? = null,
    val formError: String? = null,
    val isSaving: Boolean = false,
    val done: Boolean = false
) {
    val programmeStart: LocalDate? get() = Formatters.parseDate(programme?.startDate)
    val programmeEnd: LocalDate? get() = Formatters.parseDate(programme?.endDate)
}

class NewSessionViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(NewSessionUiState())
    val state: StateFlow<NewSessionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.programmes()) {
                is ApiResult.Success -> _state.update {
                    // One programme: pre-select it so the teacher has one thing less to do.
                    it.copy(programmes = result.data, programme = it.programme ?: result.data.singleOrNull(), isLoading = false)
                }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, loadError = result.message) }
            }
        }
    }

    fun onProgramme(p: Programme) {
        _state.update { it.copy(programme = p, programmeError = null, dateError = null, formError = null) }
    }

    fun onDate(d: LocalDate) {
        _state.update { it.copy(date = d, dateError = null, formError = null) }
    }

    fun onStart(t: LocalTime) {
        _state.update {
            // Suggest a one-hour session until the teacher picks an end time that suits them.
            val end = if (it.end == null || !it.end.isAfter(t)) t.plusHours(1).takeIf { e -> e.isAfter(t) } else it.end
            it.copy(start = t, end = end, timeError = null, formError = null)
        }
    }

    fun onEnd(t: LocalTime) {
        _state.update { it.copy(end = t, timeError = null, formError = null) }
    }

    fun consumeDone() {
        _state.update { it.copy(done = false) }
    }

    fun save() {
        val s = _state.value
        if (s.isSaving) return

        val programmeError = Validators.validateProgrammeChoice(s.programme?.programmeId)
        val dateError = Validators.validateSessionDate(s.date, Formatters.today(), s.programmeStart, s.programmeEnd)
        val timeError = Validators.validateSessionTimes(s.start, s.end)
        if (programmeError != null || dateError != null || timeError != null) {
            _state.update {
                it.copy(programmeError = programmeError, dateError = dateError, timeError = timeError, formError = null)
            }
            return
        }

        _state.update { it.copy(isSaving = true, formError = null) }
        val time = DateTimeFormatter.ofPattern("HH:mm:ss")
        val request = CreateSessionRequest(
            programmeId = s.programme!!.programmeId,
            sessionDate = s.date!!.toString(),
            startTime = s.start!!.format(time),
            endTime = s.end!!.format(time)
        )
        viewModelScope.launch {
            when (val result = repository.createSession(request)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, done = true) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, formError = result.message) }
            }
        }
    }
}
