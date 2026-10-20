package com.teampointblank.lpms.ui.sessions

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.SessionSummary
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.PillChip
import com.teampointblank.lpms.ui.components.ScreenTitle
import com.teampointblank.lpms.ui.components.SessionStatusChip
import com.teampointblank.lpms.ui.components.SmallAccentButton
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onNavigate: (String) -> Unit,
    onOpenSession: (Int) -> Unit,
    onNewSession: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    val palette = LpmsColors.palette

    MainScaffold(currentRoute = Routes.SESSIONS, onNavigate = onNavigate) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScreenTitle("Sessions") {
                SmallAccentButton("New", Icons.Filled.Add, onNewSession)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SessionScope.values().forEach { scope ->
                    PillChip(
                        label = scope.label,
                        selected = state.scope == scope,
                        onClick = { viewModel.setScope(scope) }
                    )
                }
            }
            if (state.isLoading && state.sessions != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = palette.accent
                )
            }

            val sessions = state.sessions
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    sessions == null && state.isLoading -> LoadingBox()
                    sessions == null -> ErrorState(
                        message = state.error ?: "Something went wrong. Please try again.",
                        onRetry = { viewModel.load() }
                    )
                    sessions.isEmpty() -> Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        DashedEmpty(emptyMessage(state.scope))
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sessions, key = { it.sessionId }) { session ->
                            SessionCard(session = session, onClick = { onOpenSession(session.sessionId) })
                        }
                    }
                }
            }
        }
    }
}

private fun emptyMessage(scope: SessionScope): String = when (scope) {
    SessionScope.Upcoming -> "No upcoming sessions yet."
    SessionScope.Past -> "No past sessions yet."
    else -> "No sessions yet."
}

@Composable
fun SessionCard(session: SessionSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LpmsColors.palette
    LpmsCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text(session.programmeName, color = palette.text, style = MaterialTheme.typography.titleLarge)
            Text(session.schoolName, color = palette.textMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${if (session.isToday) "Today" else Formatters.date(session.sessionDate)}, " +
                    Formatters.timeRange(session.startTime, session.endTime),
                color = palette.text,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionStatusChip(session.status)
                Text(
                    if (session.attendanceRecorded) {
                        "${session.presentCount} present, ${session.absentCount} absent"
                    } else {
                        Formatters.plural(session.totalLearners, "learner")
                    },
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
