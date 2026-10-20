package com.teampointblank.lpms.data

import com.teampointblank.lpms.domain.TokenRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

data class AuthState(val profile: UserProfile? = null, val notice: String? = null) {
    val isSignedIn: Boolean get() = profile != null
    val isParent: Boolean get() = profile?.role.equals("Parent", ignoreCase = true)
}

/** Single source of truth for "who is signed in". The navigation graph reacts to [state]. */
class AuthManager(private val store: TokenStore) {

    @Volatile
    private var token: String? = null

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun currentToken(): String? = token

    fun signIn(response: LoginResponse) {
        store.save(StoredSession(response.token, response.expiresAtUtc, response.user))
        token = response.token
        _state.value = AuthState(profile = response.user)
    }

    /** Signs out. [notice] is shown on the sign-in screen (for example "Your session has expired"). */
    fun signOut(notice: String? = null) {
        store.clear()
        token = null
        _state.value = AuthState(profile = null, notice = notice)
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun restore(): AuthState {
        val stored = store.load() ?: return AuthState()
        if (TokenRules.isExpired(stored.expiresAtUtc, Instant.now())) {
            store.clear()
            return AuthState()
        }
        token = stored.token
        return AuthState(profile = stored.profile)
    }
}
