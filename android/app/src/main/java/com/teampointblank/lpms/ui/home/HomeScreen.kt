package com.teampointblank.lpms.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.teampointblank.lpms.data.TeacherDashboard
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.HeaderBlock
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.SectionHeader
import com.teampointblank.lpms.ui.components.SmallAccentButton
import com.teampointblank.lpms.ui.components.StatTile
import com.teampointblank.lpms.ui.components.TonalButton
import com.teampointblank.lpms.ui.components.pullUp
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.sessions.SessionCard
import com.teampointblank.lpms.ui.theme.LpmsColors
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigate: (String) -> Unit,
    onOpenSession: (Int) -> Unit,
    onNewSession: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(currentRoute = Routes.HOME, onNavigate = onNavigate) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val data = state.data
            when {
                data == null && state.isLoading -> LoadingBox()
                data == null -> ErrorState(
                    message = state.error ?: "Something went wrong. Please try again.",
                    onRetry = { viewModel.load() }
                )
                else -> DashboardContent(
                    data = data,
                    isRefreshing = state.isLoading,
                    error = state.error,
                    onRefresh = { viewModel.load() },
                    onShowSessions = { onNavigate(Routes.SESSIONS) },
                    onOpenSession = onOpenSession,
                    onNewSession = onNewSession
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: TeacherDashboard,
    isRefreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onShowSessions: () -> Unit,
    onOpenSession: (Int) -> Unit,
    onNewSession: () -> Unit
) {
    val palette = LpmsColors.palette
    val firstName = data.teacherName.substringBefore(' ').ifBlank { "teacher" }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeaderBlock(bottomPadding = 64.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    data.schoolName,
                    modifier = Modifier.weight(1f),
                    color = palette.onHeaderMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Sync, contentDescription = "Refresh", tint = palette.onHeader)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "${Formatters.greeting(LocalTime.now().hour)}, $firstName",
                color = palette.onHeader,
                style = MaterialTheme.typography.displayMedium
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.pullUp(44.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("This week", data.sessionsThisWeek.toString(), Modifier.weight(1f))
                    StatTile("Today", data.sessionsToday.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Learners", data.totalLearners.toString(), Modifier.weight(1f))
                    StatTile("Attendance", Formatters.percent(data.attendanceRate), Modifier.weight(1f))
                }
            }

            if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = palette.accent)
            if (error != null) InfoBanner(text = error, isError = true)

            if (data.pendingAttendance > 0) {
                LpmsCard(onClick = onShowSessions) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "${Formatters.plural(data.pendingAttendance, "session")} still need attendance",
                            color = palette.text,
                            style = MaterialTheme.typography.titleMedium
                        )
                        TonalButton("View sessions", onClick = onShowSessions)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader("Today", modifier = Modifier.weight(1f))
                SmallAccentButton("New session", Icons.Filled.Add, onNewSession)
            }
            if (data.todaySessions.isEmpty()) {
                DashedEmpty("No sessions today.")
            } else {
                data.todaySessions.forEach { session ->
                    SessionCard(session = session, onClick = { onOpenSession(session.sessionId) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
