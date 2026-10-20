package com.teampointblank.lpms.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val current: String = "",
    val newPassword: String = "",
    val confirm: String = "",
    val showPasswords: Boolean = false,
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null,
    val formError: String? = null,
    val isSaving: Boolean = false,
    val done: Boolean = false
)

class ChangePasswordViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) {
        _state.update { it.copy(current = value.take(Validators.MAX_PASSWORD_LENGTH), currentError = null, formError = null) }
    }

    fun onNewChange(value: String) {
        _state.update { it.copy(newPassword = value.take(Validators.MAX_PASSWORD_LENGTH), newError = null, formError = null) }
    }

    fun onConfirmChange(value: String) {
        _state.update { it.copy(confirm = value.take(Validators.MAX_PASSWORD_LENGTH), confirmError = null, formError = null) }
    }

    fun toggleShow() {
        _state.update { it.copy(showPasswords = !it.showPasswords) }
    }

    fun submit() {
        val s = _state.value
        if (s.isSaving) return

        val currentError = Validators.validateCurrentPassword(s.current)
        val newError = Validators.validateNewPassword(s.newPassword, s.current)
        val confirmError = Validators.validateConfirmPassword(s.newPassword, s.confirm)
        if (currentError != null || newError != null || confirmError != null) {
            _state.update {
                it.copy(currentError = currentError, newError = newError, confirmError = confirmError, formError = null)
            }
            return
        }

        _state.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            when (val result = repository.changePassword(s.current, s.newPassword)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, done = true) }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, formError = result.message) }
            }
        }
    }

    fun consumeDone() {
        _state.update { it.copy(done = false) }
    }
}
