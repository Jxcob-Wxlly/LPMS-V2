package com.teampointblank.lpms.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.AuthManager
import com.teampointblank.lpms.data.ErrorKind
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.data.SettingsStore
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val identifierError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val notice: String? = null,
    val isLoading: Boolean = false
)

class LoginViewModel(
    private val repository: LpmsRepository,
    private val settings: SettingsStore,
    private val auth: AuthManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(
            identifier = if (settings.current.rememberUsername) settings.current.savedUsername else "",
            notice = auth.state.value.notice
        )
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        auth.clearNotice()
    }

    fun onIdentifierChange(value: String) {
        _state.update { it.copy(identifier = value.take(Validators.MAX_LOGIN_LENGTH), identifierError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value.take(Validators.MAX_PASSWORD_LENGTH), passwordError = null, formError = null) }
    }

    fun toggleShowPassword() {
        _state.update { it.copy(showPassword = !it.showPassword) }
    }

    fun signIn() {
        val current = _state.value
        if (current.isLoading) return

        val identifierError = Validators.validateIdentifier(current.identifier)
        val passwordError = Validators.validateLoginPassword(current.password)
        if (identifierError != null || passwordError != null) {
            _state.update {
                it.copy(identifierError = identifierError, passwordError = passwordError, formError = null, notice = null)
            }
            return
        }

        _state.update {
            it.copy(isLoading = true, identifierError = null, passwordError = null, formError = null, notice = null)
        }
        viewModelScope.launch {
            when (val result = repository.signIn(current.identifier, current.password)) {
                is ApiResult.Success -> {
                    settings.saveUsername(current.identifier.trim())
                    _state.update { it.copy(isLoading = false, password = "") }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            formError = result.message,
                            password = if (result.kind == ErrorKind.Unauthorized) "" else it.password
                        )
                    }
                }
            }
        }
    }
}
