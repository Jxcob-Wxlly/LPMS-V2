package com.teampointblank.lpms.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampointblank.lpms.data.Announcement
import com.teampointblank.lpms.data.ApiResult
import com.teampointblank.lpms.data.ConversationSummary
import com.teampointblank.lpms.data.LpmsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MessagesTab(val label: String) {
    Chats("Chats"),
    Announcements("Announcements")
}

data class MessagesUiState(
    val tab: MessagesTab = MessagesTab.Chats,
    val conversations: List<ConversationSummary>? = null,
    val announcements: List<Announcement>? = null,
    val isLoading: Boolean = true,
    val chatsError: String? = null,
    val announcementsError: String? = null
)

/** The Messages tab: the signed-in user's conversations and the announcements they may read. */
class MessagesViewModel(
    private val repository: LpmsRepository,
    val isParent: Boolean
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun setTab(tab: MessagesTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun load() {
        job?.cancel()
        _state.update { it.copy(isLoading = true) }
        job = viewModelScope.launch {
            when (val chats = repository.conversations()) {
                is ApiResult.Success ->
                    _state.update { it.copy(conversations = chats.data, chatsError = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(chatsError = chats.message) }
            }
            when (val notices = repository.announcements()) {
                is ApiResult.Success ->
                    _state.update { it.copy(announcements = notices.data, announcementsError = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(announcementsError = notices.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}
