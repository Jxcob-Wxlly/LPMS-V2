package com.teampointblank.lpms.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SchoolOption
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val schools: List<SchoolOption> = emptyList(),
    val school: SchoolOption? = null,
    val schoolsLoading: Boolean = true,
    val nameError: String? = null,
    val emailError: String? = null,
    val schoolError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val isSaving: Boolean = false
)

class RegisterViewModel(private val repository: LpmsRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    init {
        loadSchools()
    }

    fun loadSchools() {
        _state.update { it.copy(schoolsLoading = true, formError = null) }
        viewModelScope.launch {
            when (val result = repository.schoolOptions()) {
                is ApiResult.Success -> _state.update { it.copy(schools = result.data, schoolsLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(schoolsLoading = false, formError = result.message) }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(fullName = value.take(Validators.MAX_NAME_LENGTH), nameError = null, formError = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value.take(Validators.MAX_LOGIN_LENGTH), emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value.take(Validators.MAX_PASSWORD_LENGTH), passwordError = null, formError = null) }
    }

    fun onSchoolChange(school: SchoolOption) {
        _state.update { it.copy(school = school, schoolError = null, formError = null) }
    }

    fun toggleShowPassword() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    /** On success the repository signs the user in and the navigation host moves to Home. */
    fun submit() {
        val current = _state.value
        if (current.isSaving) return

        val nameError = Validators.validateFullName(current.fullName)
        val emailError = Validators.validateEmail(current.email)
        val schoolError = Validators.validateSchoolChoice(current.school?.schoolId)
        val passwordError = Validators.validateRegisterPassword(current.password)
        if (listOf(nameError, emailError, schoolError, passwordError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError, emailError = emailError,
                    schoolError = schoolError, passwordError = passwordError, formError = null
                )
            }
            return
        }

        _state.update { it.copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            val result = repository.register(current.fullName, current.email, current.school!!.schoolId, current.password)
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false, password = "") }
                is ApiResult.Failure -> _state.update { it.copy(isSaving = false, formError = result.message) }
            }
        }
    }
}
