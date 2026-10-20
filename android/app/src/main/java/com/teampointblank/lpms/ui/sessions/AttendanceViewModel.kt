package com.teampointblank.lpms.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SessionDetail
import com.teampointblank.lpms.domain.AttendanceLogic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AttendanceUiState(
    val detail: SessionDetail? = null,
    /** learnerId -> "Present" | "Absent" for the learners the teacher has marked so far. */
    val draft: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    /** One-shot message (shown once as a snackbar, then cleared). */
    val message: String? = null
)

class AttendanceViewModel(
    private val repository: LpmsRepository,
    private val sessionId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceUiState())
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.session(sessionId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        detail = result.data,
                        draft = AttendanceLogic.initialDraft(result.data.learners),
                        isLoading = false,
                        loadError = null
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, loadError = result.message) }
            }
        }
    }

    fun setStatus(learnerId: Int, status: String) {
        _state.update {
            it.copy(draft = AttendanceLogic.setStatus(it.draft, learnerId, status), saveError = null)
        }
    }

    fun markAll(status: String) {
        val roster = _state.value.detail?.learners ?: return
        _state.update { it.copy(draft = AttendanceLogic.markAll(roster, status), saveError = null) }
    }

    fun save() {
        val current = _state.value
        val detail = current.detail ?: return
        if (current.isSaving) return

        val problem = AttendanceLogic.validateForSave(detail.learners, current.draft)
        if (problem != null) {
            _state.update { it.copy(saveError = problem) }
            return
        }

        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val entries = AttendanceLogic.entriesToSave(detail.learners, current.draft)
            when (val result = repository.saveAttendance(sessionId, entries)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        detail = result.data,
                        draft = AttendanceLogic.initialDraft(result.data.learners),
                        isSaving = false,
                        message = "Attendance saved."
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, saveError = result.message) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
