package com.teampointblank.lpms.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.teampointblank.lpms.R
import com.teampointblank.lpms.data.ThemeMode

// ---------------------------------------------------------------- fonts

/** Headings: Bricolage Grotesque. Everything else: Figtree. Both are open-licence fonts bundled in res/font. */
val HeadingFont = FontFamily(
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold)
)

val BodyFont = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold)
)

private val LpmsTypography = Typography(
    displayLarge = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
)

private val LpmsShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ---------------------------------------------------------------- palette

/** The colours used in the design mockups (teal, amber and cream), for light and dark. */
@Immutable
class LpmsPalette(
    val isDark: Boolean,
    /** Screen background. */
    val background: Color,
    /** Cards and grouped lists. */
    val card: Color,
    /** Chips and inner buttons that sit on a card. */
    val chip: Color,
    /** Teal header block and the sign-in backdrop. */
    val header: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    /** The amber call-to-action colour. */
    val accent: Color,
    val onAccent: Color,
    /** Selected chip / toggle / nav item. */
    val selected: Color,
    val onSelected: Color,
    val text: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val nav: Color,
    val tonalButton: Color,
    val onTonalButton: Color,
    val danger: Color
)

private val LightPalette = LpmsPalette(
    isDark = false,
    background = Color(0xFFFAF6ED),
    card = Color(0xFFFFFFFF),
    chip = Color(0xFFF0EBDF),
    header = Color(0xFF00353D),
    onHeader = Color(0xFFFAF6ED),
    onHeaderMuted = Color(0xFFA9C2C6),
    accent = Color(0xFFF7B52D),
    onAccent = Color(0xFF1B1500),
    selected = Color(0xFF04535F),
    onSelected = Color(0xFFFFFFFF),
    text = Color(0xFF062A30),
    textMuted = Color(0xFF5B6E72),
    border = Color(0xFFD9D3C4),
    divider = Color(0xFFE6E0D3),
    nav = Color(0xFFFFFFFF),
    tonalButton = Color(0xFFD2EEF1),
    onTonalButton = Color(0xFF00353D),
    danger = Color(0xFFD64545)
)

private val DarkPalette = LpmsPalette(
    isDark = true,
    background = Color(0xFF051419),
    card = Color(0xFF0B1F26),
    chip = Color(0xFF12292F),
    header = Color(0xFF00353D),
    onHeader = Color(0xFFFAF6ED),
    onHeaderMuted = Color(0xFFA9C2C6),
    accent = Color(0xFFF7B52D),
    onAccent = Color(0xFF1B1500),
    selected = Color(0xFFF7B52D),
    onSelected = Color(0xFF1B1500),
    text = Color(0xFFF7F3EA),
    textMuted = Color(0xFF9DB3B8),
    border = Color(0xFF2A4047),
    divider = Color(0xFF173038),
    nav = Color(0xFF092125),
    tonalButton = Color(0xFF12333A),
    onTonalButton = Color(0xFFF7F3EA),
    danger = Color(0xFFF2625E)
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

private fun materialColors(p: LpmsPalette): ColorScheme =
    if (p.isDark) {
        darkColorScheme(
            primary = p.selected, onPrimary = p.onSelected,
            primaryContainer = p.chip, onPrimaryContainer = p.text,
            secondary = Color(0xFF7FC8D0), onSecondary = p.header,
            secondaryContainer = p.tonalButton, onSecondaryContainer = p.text,
            tertiary = p.accent, onTertiary = p.onAccent,
            tertiaryContainer = Color(0xFF3A2D08), onTertiaryContainer = Color(0xFFFFE3A3),
            background = p.background, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceVariant = p.chip, onSurfaceVariant = p.textMuted,
            outline = p.border, outlineVariant = p.divider,
            error = p.danger, onError = Color.White,
            errorContainer = Color(0xFF4A1C1C), onErrorContainer = Color(0xFFFFD9D6),
            surfaceContainerLowest = p.background, surfaceContainerLow = p.card,
            surfaceContainer = p.card, surfaceContainerHigh = p.chip, surfaceContainerHighest = p.chip
        )
    } else {
        lightColorScheme(
            primary = p.selected, onPrimary = p.onSelected,
            primaryContainer = p.tonalButton, onPrimaryContainer = p.onTonalButton,
            secondary = Color(0xFF2F7A85), onSecondary = Color.White,
            secondaryContainer = p.tonalButton, onSecondaryContainer = p.onTonalButton,
            tertiary = Color(0xFF8A5A00), onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFE8B5), onTertiaryContainer = Color(0xFF3A2600),
            background = p.background, onBackground = p.text,
            surface = p.card, onSurface = p.text,
            surfaceVariant = p.chip, onSurfaceVariant = p.textMuted,
            outline = p.border, outlineVariant = p.divider,
            error = p.danger, onError = Color.White,
            errorContainer = Color(0xFFFBE0DE), onErrorContainer = Color(0xFF5A1010),
            surfaceContainerLowest = p.card, surfaceContainerLow = p.card,
            surfaceContainer = p.chip, surfaceContainerHigh = p.chip, surfaceContainerHighest = p.chip
        )
    }

/** Extra colours for statuses (Completed / Pending / Upcoming, Present / Absent) that Material lacks. */
data class StatusColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color
)

private val LightStatus = StatusColors(
    successContainer = Color(0xFFD5EFE0),
    onSuccessContainer = Color(0xFF0E4025),
    warningContainer = Color(0xFFFFE8B5),
    onWarningContainer = Color(0xFF4F3400),
    infoContainer = Color(0xFFD2EEF1),
    onInfoContainer = Color(0xFF00353D)
)

private val DarkStatus = StatusColors(
    successContainer = Color(0xFF14432E),
    onSuccessContainer = Color(0xFFBDEBD0),
    warningContainer = Color(0xFF3F3008),
    onWarningContainer = Color(0xFFFFD98A),
    infoContainer = Color(0xFF12333A),
    onInfoContainer = Color(0xFFCDEAEE)
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatus }

object LpmsColors {
    val status: StatusColors
        @Composable get() = LocalStatusColors.current

    val palette: LpmsPalette
        @Composable get() = LocalPalette.current
}

@Composable
fun LpmsTheme(themeMode: ThemeMode, largeText: Boolean, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val palette = if (dark) DarkPalette else LightPalette

    val baseDensity = LocalDensity.current
    val density = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * (if (largeText) 1.2f else 1f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                // Status-bar icon colour is set per screen (see StatusBarIcons in Components.kt).
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalDensity provides density,
        LocalPalette provides palette,
        LocalStatusColors provides (if (dark) DarkStatus else LightStatus)
    ) {
        MaterialTheme(
            colorScheme = materialColors(palette),
            typography = LpmsTypography,
            shapes = LpmsShapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
