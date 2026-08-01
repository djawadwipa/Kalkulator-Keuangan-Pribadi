package id.djawadwipa.kalkulatorkeuangan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import id.djawadwipa.kalkulatorkeuangan.data.AppThemeMode

val Navy = Color(0xFF0F172A)
val NavyDark = Color(0xFF07111F)
val Emerald = Color(0xFF10B981)
val EmeraldDark = Color(0xFF047857)
val OffWhite = Color(0xFFF8FAFC)
val Slate = Color(0xFF334155)
val Amber = Color(0xFFF59E0B)

private val LightScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF022C22),
    secondary = Navy,
    onSecondary = Color.White,
    background = OffWhite,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    error = Color(0xFFB91C1C),
)

private val DarkScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF93C5FD),
    onSecondary = NavyDark,
    background = NavyDark,
    onBackground = OffWhite,
    surface = Navy,
    onSurface = OffWhite,
    error = Color(0xFFFCA5A5),
)

@Composable
fun KalkulatorKeuanganTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
