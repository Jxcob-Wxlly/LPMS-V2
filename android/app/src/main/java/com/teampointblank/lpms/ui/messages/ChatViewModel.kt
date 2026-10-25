package com.teampointblank.lpms.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.ChatMessage
import com.teampointblank.lpms.data.ConversationSummary
import com.teampointblank.lpms.data.LpmsRepository
import com.teampointblank.lpms.domain.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val header: ConversationSummary? = null,
    val messages: List<ChatMessage>? = null,
    val draft: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSending: Boolean = false,
    val sendError: String? = null
)

/** One conversation about one learner between a parent and the school staff. */
class ChatViewModel(
    private val repository: LpmsRepository,
    private val learnerId: Int,
    private val parentUserId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.thread(learnerId, parentUserId)) {
                is ApiResult.Success -> _state.update { it.copy(messages = result.data, isLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(isLoading = false, loadError = result.message) }
            }
            // The header (who is on the other side) comes from the conversation list; the chat still works without it.
            if (_state.value.header == null) {
                val list = repository.conversations()
                if (list is ApiResult.Success) {
                    val match = list.data.firstOrNull { it.learnerId == learnerId && it.parentUserId == parentUserId }
                    _state.update { it.copy(header = match) }
                }
            }
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value, sendError = null) }
    }

    fun send() {
        val current = _state.value
        if (current.isSending) return
        val problem = Validators.validateMessage(current.draft)
        if (problem != null) {
            _state.update { it.copy(sendError = problem) }
            return
        }
        _state.update { it.copy(isSending = true, sendError = null) }
        viewModelScope.launch {
            when (val result = repository.sendMessage(learnerId, parentUserId, current.draft)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        messages = (it.messages ?: emptyList()) + result.data,
                        draft = "",
                        isSending = false
                    )
                }
                is ApiResult.Failure -> _state.update { it.copy(isSending = false, sendError = result.message) }
            }
        }
    }
}
