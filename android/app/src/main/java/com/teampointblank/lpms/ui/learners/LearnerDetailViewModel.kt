package com.teampointblank.lpms.ui.learners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.Assessment
import com.teampointblank.lpms.data.LearnerProgress
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The add / edit assessment dialog. [editingId] is null when adding a new assessment. */
data class AssessmentForm(
    val editingId: Int? = null,
    val score: String = "",
    val date: LocalDate? = null,
    val notes: String = "",
    val scoreError: String? = null,
    val dateError: String? = null,
    val notesError: String? = null,
    val formError: String? = null,
    val isSaving: Boolean = false
)

data class LearnerDetailUiState(
    val progress: LearnerProgress? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Non-null while the add/edit dialog is open. */
    val form: AssessmentForm? = null,
    /** Non-null while the delete confirmation is open. */
    val deleteTarget: Assessment? = null,
    val isDeleting: Boolean = false,
    val message: String? = null
)

class LearnerDetailViewModel(
    private val repository: LpmsRepository,
    private val learnerId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(LearnerDetailUiState())
    val state: StateFlow<LearnerDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = repository.learnerProgress(learnerId)) {
                is ApiResult.Success ->
                    _state.update { it.copy(progress = result.data, isLoading = false, error = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    // ----- add / edit dialog -----

    fun openAdd() {
        _state.update { it.copy(form = AssessmentForm(date = Formatters.today())) }
    }

    fun openEdit(assessment: Assessment) {
        _state.update {
            it.copy(
                form = AssessmentForm(
                    editingId = assessment.progressId,
                    score = Formatters.score(assessment.assessmentScore),
                    date = Formatters.parseDate(assessment.assessmentDate),
                    notes = assessment.notes ?: ""
                )
            )
        }
    }

    fun closeForm() {
        _state.update { it.copy(form = null) }
    }

    fun onScoreChange(value: String) {
        updateForm { it.copy(score = Validators.filterScoreInput(value), scoreError = null, formError = null) }
    }

    fun onDateChange(date: LocalDate) {
        updateForm { it.copy(date = date, dateError = null, formError = null) }
    }

    fun onNotesChange(value: String) {
        updateForm {
            it.copy(
                notes = value.take(Validators.MAX_NOTES_LENGTH),
                notesError = null,
                formError = null
            )
        }
    }

    fun saveForm() {
        val form = _state.value.form ?: return
        if (form.isSaving) return

        val score = Validators.parseScore(form.score)
        val dateError = Validators.validateAssessmentDate(form.date, Formatters.today())
        val notesError = Validators.validateNotes(form.notes)
        val scoreValue = score.value
        val date = form.date
        if (scoreValue == null || date == null || dateError != null || notesError != null) {
            updateForm { it.copy(scoreError = score.error, dateError = dateError, notesError = notesError) }
            return
        }

        updateForm { it.copy(isSaving = true, scoreError = null, dateError = null, notesError = null, formError = null) }
        val notes = form.notes.trim().ifEmpty { null }
        viewModelScope.launch {
            val editingId = form.editingId
            val result = if (editingId == null) {
                repository.addAssessment(learnerId, scoreValue, date.toString(), notes)
            } else {
                repository.updateAssessment(editingId, scoreValue, date.toString(), notes)
            }
            when (result) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            form = null,
                            message = if (editingId == null) "Assessment added." else "Assessment updated."
                        )
                    }
                    load()
                }
                is ApiResult.Failure -> updateForm { it.copy(isSaving = false, formError = result.message) }
            }
        }
    }

    // ----- delete -----

    fun askDelete(assessment: Assessment) {
        _state.update { it.copy(deleteTarget = assessment) }
    }

    fun cancelDelete() {
        _state.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (val result = repository.deleteAssessment(target.progressId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isDeleting = false, deleteTarget = null, message = "Assessment deleted.") }
                    load()
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isDeleting = false, deleteTarget = null, message = result.message)
                }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateForm(transform: (AssessmentForm) -> AssessmentForm) {
        _state.update { current ->
            val form = current.form
            if (form == null) current else current.copy(form = transform(form))
        }
    }
}
