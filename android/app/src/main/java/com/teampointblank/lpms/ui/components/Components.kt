@file:OptIn(ExperimentalMaterial3Api::class)

package com.teampointblank.lpms.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.layout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.teampointblank.lpms.R
import com.teampointblank.lpms.ui.navigation.Routes
import com.teampointblank.lpms.ui.theme.LpmsColors

/** Loading / error wrapper used by every screen that fetches data. */
data class Resource<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ------------------------------------------------------------------ small helpers

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Chooses dark or light status-bar icons for the screen being shown (teal headers need light icons). */
@Composable
fun StatusBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
            }
        }
    }
}

/** Pulls a composable up by [amount] so it overlaps the one above it (stat tiles over the header). */
fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val up = amount.roundToPx()
    layout(placeable.width, (placeable.height - up).coerceAtLeast(0)) { placeable.place(0, -up) }
}

fun Modifier.dashedBorder(color: Color, radius: Dp, strokeWidth: Dp = 1.dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = strokeWidth.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
    )
}

// ------------------------------------------------------------------ headers and scaffolds

/** Teal header with rounded bottom corners. Content sits under the status bar. */
@Composable
fun HeaderBlock(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    StatusBarIcons(darkIcons = false)
    val palette = LpmsColors.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(palette.header)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding),
        content = content
    )
}

/** Header for sub-screens: "< Back" row, a big title and optional actions. */
@Composable
fun LpmsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LpmsColors.palette
    HeaderBlock(bottomPadding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = palette.onHeader,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Back", color = palette.onHeader, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.weight(1f))
            actions()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            color = palette.onHeader,
            style = MaterialTheme.typography.headlineLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Big plain screen title used by Sessions, Learners and Settings (no header block). */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    StatusBarIcons(darkIcons = !LpmsColors.palette.isDark)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = LpmsColors.palette.text,
            style = MaterialTheme.typography.displayMedium
        )
        trailing()
    }
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, "Home", Icons.Outlined.Home),
    BottomItem(Routes.SESSIONS, "Sessions", Icons.Outlined.CalendarMonth),
    BottomItem(Routes.LEARNERS, "Learners", Icons.Outlined.People),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
)

private val parentBottomItems = listOf(
    BottomItem(Routes.PARENT_HOME, "My children", Icons.Outlined.People),
    BottomItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
)

@Composable
fun LpmsBottomBar(currentRoute: String, onNavigate: (String) -> Unit, isParent: Boolean = false) {
    val palette = LpmsColors.palette
    Column {
        HorizontalDivider(color = palette.divider)
        NavigationBar(containerColor = palette.nav, tonalElevation = 0.dp) {
            (if (isParent) parentBottomItems else bottomItems).forEach { item ->
                val selected = item.route == currentRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = { if (!selected) onNavigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = {
                        Text(
                            item.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = palette.selected,
                        selectedTextColor = palette.selected,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = palette.textMuted,
                        unselectedTextColor = palette.textMuted
                    )
                )
            }
        }
    }
}

/** Scaffold for the main tabs. Each screen draws its own header, so insets are handled there. */
@Composable
fun MainScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    snackbarHostState: SnackbarHostState? = null,
    isParent: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = LpmsColors.palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { LpmsBottomBar(currentRoute, onNavigate, isParent) },
        snackbarHost = { if (snackbarHostState != null) LpmsSnackbarHost(snackbarHostState) },
        content = content
    )
}

/** White "toast" style message (as in the mockups). */
@Composable
fun LpmsSnackbarHost(state: SnackbarHostState) {
    SnackbarHost(hostState = state) { data ->
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            contentColor = Color(0xFF062A30),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.size(14.dp))
                Text(data.visuals.message, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ------------------------------------------------------------------ state boxes

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LpmsColors.palette.accent)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = LpmsColors.palette.danger,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = LpmsColors.palette.text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(16.dp))
        TonalButton("Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f))
    }
}

/** Dashed empty box, e.g. "No sessions yet." */
@Composable
fun DashedEmpty(message: String, modifier: Modifier = Modifier, title: String? = null) {
    val palette = LpmsColors.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(palette.border, 28.dp)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
        }
        Text(message, color = palette.textMuted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) =
    DashedEmpty(message = message, modifier = modifier, title = title)

/** A coloured message box. Use [isError] for problems, otherwise it is informational. */
@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else LpmsColors.status.infoContainer
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else LpmsColors.status.onInfoContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isError) Icons.Filled.Warning else Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ------------------------------------------------------------------ text styles / cards

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.headlineSmall,
        color = LpmsColors.palette.text
    )
}

/** Small spaced capitals such as APPEARANCE or AVERAGE SCORE. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        color = LpmsColors.palette.textMuted
    )
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LpmsColors.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(palette.card)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 0.8.sp,
            color = palette.textMuted
        )
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.displaySmall, color = palette.text)
    }
}

/** Rounded card used for list rows. */
@Composable
fun LpmsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LpmsColors.palette
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(palette.card)
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content
    )
}

/** Small coloured label. Always carries text, so meaning never depends on colour alone. */
@Composable
fun StatusChip(label: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Amber pill used for a headline label such as "Excellent" under a learner's name. */
@Composable
fun AccentChip(label: String, modifier: Modifier = Modifier) {
    val palette = LpmsColors.palette
    Surface(modifier = modifier, color = palette.accent, contentColor = palette.onAccent, shape = RoundedCornerShape(50)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SessionStatusChip(status: String) {
    val colors = LpmsColors.status
    when (status) {
        "Completed" -> StatusChip("Completed", colors.successContainer, colors.onSuccessContainer)
        "Pending" -> StatusChip("Attendance needed", colors.warningContainer, colors.onWarningContainer)
        else -> StatusChip(status.ifBlank { "Upcoming" }, colors.infoContainer, colors.onInfoContainer)
    }
}

@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier, size: Dp = 54.dp) {
    val palette = LpmsColors.palette
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.accent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = palette.onAccent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LpmsLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF01505A)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_lpms_logo),
            contentDescription = "LPMS logo",
            modifier = Modifier.size(64.dp)
        )
    }
}

// ------------------------------------------------------------------ buttons

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val palette = LpmsColors.palette
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(50),
        color = if (enabled || loading) palette.accent else palette.accent.copy(alpha = 0.35f),
        contentColor = palette.onAccent
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = palette.onAccent)
            } else {
                Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LpmsColors.palette
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(50),
        color = if (enabled) palette.tonalButton else palette.tonalButton.copy(alpha = 0.5f),
        contentColor = palette.onTonalButton
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/** Small amber pill such as "+ Add" or "+ New". */
@Composable
fun SmallAccentButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LpmsColors.palette
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(50),
        color = palette.accent,
        contentColor = palette.onAccent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}

/** Selectable pill (Upcoming / Past / All, Phone / Light / Dark). */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LpmsColors.palette
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(50),
        color = if (selected) palette.selected else palette.chip,
        contentColor = if (selected) palette.onSelected else palette.textMuted
    ) {
        Box(modifier = Modifier.padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// ------------------------------------------------------------------ form fields

/** Label above a rounded outlined field, as in the sign-in and form mockups. */
@Composable
fun LpmsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    hint: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    val palette = LpmsColors.palette
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = palette.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            placeholder = placeholder?.let { { Text(it, color = palette.textMuted) } },
            trailingIcon = trailing,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(22.dp),
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
        val support = error ?: hint
        if (support != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                support,
                modifier = Modifier.padding(start = 4.dp),
                color = if (error != null) palette.danger else palette.textMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Amber text action inside a field, e.g. "Show". */
@Composable
fun FieldTextAction(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = LpmsColors.palette.accent, style = MaterialTheme.typography.titleMedium)
    }
}

// ------------------------------------------------------------------ settings rows

@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    LpmsCard(modifier = modifier, content = content)
}

@Composable
fun GroupDivider() {
    HorizontalDivider(color = LpmsColors.palette.divider)
}

@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    labelColor: Color = LpmsColors.palette.text
) {
    val palette = LpmsColors.palette
    val base = modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp)
    Row(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp))
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(
                value,
                color = palette.textMuted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    val palette = LpmsColors.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = palette.text, fontSize = 19.sp, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = palette.textMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onSelected,
                checkedTrackColor = palette.selected,
                checkedBorderColor = palette.selected,
                uncheckedThumbColor = if (palette.isDark) palette.background else palette.card,
                uncheckedTrackColor = if (palette.isDark) Color(0xFF2E3E43) else Color(0xFFDCD5C6),
                uncheckedBorderColor = if (palette.isDark) Color(0xFF2E3E43) else Color(0xFFDCD5C6)
            )
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel"
) {
    val palette = LpmsColors.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.text,
        textContentColor = palette.textMuted,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = palette.accent.takeIf { palette.isDark } ?: palette.selected)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel, color = palette.textMuted) } }
    )
}
