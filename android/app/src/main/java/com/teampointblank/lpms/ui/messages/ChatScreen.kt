package com.teampointblank.lpms.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.ChatMessage
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsTopBar
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun ChatScreen(viewModel: ChatViewModel, isParent: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LpmsColors.palette
    val listState = rememberLazyListState()
    val messages = state.messages ?: emptyList()

    // Show the newest message whenever the list grows.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val header = state.header
    val title = when {
        header == null -> "Conversation"
        isParent -> header.learnerName
        else -> header.parentName
    }
    val subtitle = when {
        header == null -> null
        isParent -> "${header.schoolName} staff"
        else -> "Parent of ${header.learnerName} · ${header.grade}"
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            LpmsTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.load() }, enabled = !state.isSending) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Reload", tint = palette.onHeader)
                    }
                },
                subtitle = subtitle
            )
        },
        bottomBar = {
            MessageInput(
                draft = state.draft,
                error = state.sendError,
                isSending = state.isSending,
                enabled = state.messages != null,
                onDraftChange = viewModel::setDraft,
                onSend = viewModel::send
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.messages == null && state.isLoading -> LoadingBox()
                state.messages == null -> ErrorState(
                    message = state.loadError ?: "Something went wrong. Please try again.",
                    onRetry = { viewModel.load() }
                )
                messages.isEmpty() -> Column(modifier = Modifier.padding(20.dp)) {
                    DashedEmpty("No messages yet. Say hello below.")
                }
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
                    if (state.loadError != null) {
                        InfoBanner(text = state.loadError ?: "", isError = true, modifier = Modifier.padding(20.dp))
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.messageId }) { message -> MessageBubble(message) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val palette = LpmsColors.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(22.dp),
            color = if (message.isMine) palette.accent else palette.card,
            contentColor = if (message.isMine) palette.onAccent else palette.text
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (!message.isMine) {
                    Text(
                        "${message.senderName} · ${message.senderRole}",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Text(message.body, style = MaterialTheme.typography.bodyLarge)
                Text(
                    Formatters.messageTime(message.sentAt, Formatters.today()),
                    color = if (message.isMine) palette.onAccent.copy(alpha = 0.7f) else palette.textMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun MessageInput(
    draft: String,
    error: String?,
    isSending: Boolean,
    enabled: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val palette = LpmsColors.palette
    Surface(shadowElevation = 8.dp, color = palette.nav) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (error != null) {
                Text(error, color = palette.danger, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled && !isSending,
                    placeholder = { Text("Write a message", color = palette.textMuted) },
                    maxLines = 4,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        disabledTextColor = palette.textMuted,
                        errorTextColor = palette.text,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.border,
                        disabledBorderColor = palette.border,
                        errorBorderColor = palette.danger,
                        cursorColor = palette.accent,
                        errorCursorColor = palette.danger,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    )
                )
                IconButton(onClick = onSend, enabled = enabled && !isSending && draft.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (draft.isNotBlank()) palette.accent else palette.textMuted
                    )
                }
            }
        }
    }
}
