package com.teampointblank.lpms.ui.learners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.data.LearnerSummary
import com.teampointblank.lpms.domain.Formatters
import com.teampointblank.lpms.ui.components.Avatar
import com.teampointblank.lpms.ui.components.DashedEmpty
import com.teampointblank.lpms.ui.components.ErrorState
import com.teampointblank.lpms.ui.components.InfoBanner
import com.teampointblank.lpms.ui.components.LoadingBox
import com.teampointblank.lpms.ui.components.LpmsCard
import com.teampointblank.lpms.ui.components.MainScaffold
import com.teampointblank.lpms.ui.components.ScreenTitle
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.theme.LpmsColors

@Composable
fun LearnersScreen(
    viewModel: LearnersViewModel,
    onNavigate: (String) -> Unit,
    onOpenLearner: (Int) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val palette = LpmsColors.palette
    LaunchedEffect(Unit) { viewModel.load() }

    MainScaffold(currentRoute = Routes.LEARNERS, onNavigate = onNavigate) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScreenTitle("Learners")
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(50),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text("Search by first or last name", color = palette.textMuted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = palette.textMuted) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearQuery() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = palette.textMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    cursorColor = palette.accent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.load()
                })
            )
            if (state.isLoading && state.learners != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    color = palette.accent
                )
            }

            val learners = state.learners
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    learners == null && state.isLoading -> LoadingBox()
                    learners == null -> ErrorState(
                        message = state.error ?: "Something went wrong. Please try again.",
                        onRetry = { viewModel.load() }
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.error != null) {
                            item { InfoBanner(text = state.error ?: "", isError = true) }
                        }
                        if (learners.isEmpty()) {
                            item {
                                DashedEmpty(
                                    message = if (state.query.isBlank()) {
                                        "No learners yet."
                                    } else {
                                        "No learner's name contains \"${state.query.trim()}\"."
                                    }
                                )
                            }
                        } else {
                            items(learners, key = { it.learnerId }) { learner ->
                                LearnerCard(learner = learner, onClick = { onOpenLearner(learner.learnerId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnerCard(learner: LearnerSummary, onClick: () -> Unit) {
    val palette = LpmsColors.palette
    LpmsCard(onClick = onClick, shape = RoundedCornerShape(32.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(Formatters.initials(learner.firstName, learner.lastName), size = 58.dp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Formatters.fullName(learner.firstName, learner.lastName),
                    color = palette.text,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp)
                )
                Text(
                    "${learner.grade} · ${learner.schoolName}",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${Formatters.score(learner.averageScore)} avg",
                    color = palette.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${Formatters.percent(learner.attendanceRate)} att.",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
