package dev.lssoftware.digitalmenu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrangeLight,
    onPrimary = Color(0xFF3A1400),
    secondary = Amber,
    tertiary = OrangeDark,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onBackground = NightOnSurface,
    onSurface = NightOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    secondary = OrangeDark,
    tertiary = Amber,
    background = WarmWhite,
    surface = WarmSurface,
    surfaceVariant = WarmSurfaceVariant,
    onBackground = InkDark,
    onSurface = InkDark,
    onSurfaceVariant = InkMuted,
)

@Composable
fun DigitalMenuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the warm brand palette stays consistent across devices.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}