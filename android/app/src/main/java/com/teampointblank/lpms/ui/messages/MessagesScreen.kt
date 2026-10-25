package com.teampointblank.lpms.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.Announcement
import com.teampointblank.lpms.data.ConversationSummary
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.PillChip
import com.teampointblank.lpms.ui.components.ScreenTitle
import com.teampointblank.lpms.ui.components.StatusChip
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onNavigate: (String) -> Unit,
    onOpenChat: (learnerId: Int, parentUserId: Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    val palette = LpmsColors.palette

    MainScaffold(currentRoute = Routes.MESSAGES, onNavigate = onNavigate, isParent = viewModel.isParent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScreenTitle("Messages")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MessagesTab.values().forEach { tab ->
                    PillChip(
                        label = tab.label,
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) }
                    )
                }
            }

            val hasContent = if (state.tab == MessagesTab.Chats) state.conversations != null else state.announcements != null
            if (state.isLoading && hasContent) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = palette.accent
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (state.tab) {
                    MessagesTab.Chats -> ChatsList(
                        conversations = state.conversations,
                        isLoading = state.isLoading,
                        error = state.chatsError,
                        isParent = viewModel.isParent,
                        onRetry = { viewModel.load() },
                        onOpen = onOpenChat
                    )
                    MessagesTab.Announcements -> AnnouncementsList(
                        announcements = state.announcements,
                        isLoading = state.isLoading,
                        error = state.announcementsError,
                        onRetry = { viewModel.load() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatsList(
    conversations: List<ConversationSummary>?,
    isLoading: Boolean,
    error: String?,
    isParent: Boolean,
    onRetry: () -> Unit,
    onOpen: (Int, Int) -> Unit
) {
    when {
        conversations == null && isLoading -> LoadingBox()
        conversations == null -> ErrorState(
            message = error ?: "Something went wrong. Please try again.",
            onRetry = onRetry
        )
        conversations.isEmpty() -> Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            DashedEmpty(
                if (isParent) "You have no children linked to your account yet."
                else "There are no parents linked to learners at your school yet."
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations, key = { "${it.learnerId}-${it.parentUserId}" }) { conversation ->
                ConversationCard(
                    conversation = conversation,
                    isParent = isParent,
                    onClick = { onOpen(conversation.learnerId, conversation.parentUserId) }
                )
            }
        }
    }
}

@Composable
private fun ConversationCard(conversation: ConversationSummary, isParent: Boolean, onClick: () -> Unit) {
    val palette = LpmsColors.palette
    LpmsCard(onClick = onClick) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isParent) conversation.learnerName else conversation.parentName,
                        color = palette.text,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isParent) "${conversation.schoolName} staff"
                        else "Parent of ${conversation.learnerName} · ${conversation.grade}",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (conversation.lastSentAt != null) {
                    Text(
                        Formatters.messageTime(conversation.lastSentAt, Formatters.today()),
                        color = palette.textMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                conversation.lastMessage ?: "No messages yet. Tap to start the conversation.",
                color = if (conversation.lastMessage != null) palette.text else palette.textMuted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnnouncementsList(
    announcements: List<Announcement>?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    when {
        announcements == null && isLoading -> LoadingBox()
        announcements == null -> ErrorState(
            message = error ?: "Something went wrong. Please try again.",
            onRetry = onRetry
        )
        announcements.isEmpty() -> Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            DashedEmpty("No announcements yet.")
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(announcements, key = { it.announcementId }) { announcement ->
                AnnouncementCard(announcement)
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val palette = LpmsColors.palette
    val colors = LpmsColors.status
    LpmsCard {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    announcement.title,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    color = palette.text,
                    style = MaterialTheme.typography.titleLarge
                )
                StatusChip(
                    announcement.audience.ifBlank { "Everyone" },
                    colors.infoContainer,
                    colors.onInfoContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(announcement.body, color = palette.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(
                    announcement.authorName.ifBlank { null },
                    Formatters.messageTime(announcement.createdAt, Formatters.today()).ifBlank { null },
                    announcement.schoolName
                ).joinToString(" · "),
                color = palette.textMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
