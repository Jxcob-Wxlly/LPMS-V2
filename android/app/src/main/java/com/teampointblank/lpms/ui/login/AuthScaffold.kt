package com.teampointblank.lpms.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.teampointblank.lpms.ui.components.LpmsLogo
import com.teampointblank.lpms.ui.components.StatusBarIcons
import com.teampointblank.lpms.ui.theme.LpmsColors

/**
 * Shared look of the sign-in and create-account screens: a teal header with a big title and a
 * rounded sheet that holds the form.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    showLogo: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    StatusBarIcons(darkIcons = false)
    val palette = LpmsColors.palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.header)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = if (showLogo) 28.dp else 96.dp, bottom = 28.dp)
        ) {
            if (showLogo) {
                LpmsLogo()
                Spacer(Modifier.height(36.dp))
            }
            Text(
                title,
                color = palette.onHeader,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = palette.onHeaderMuted,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                )
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp))
                .background(palette.background)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            content = content
        )
    }
}
