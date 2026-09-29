package com.example.ui.theme

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
    primary = OfficeDarkPrimary,
    onPrimary = Color(0xFF002F5E),
    primaryContainer = OfficeBlueDark,
    onPrimaryContainer = Color(0xFFCCE4FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    background = OfficeDarkBackground,
    onBackground = Color(0xFFECEFF1),
    surface = OfficeDarkSurface,
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = OfficeDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4D5EA),
    outline = Color(0xFF4A5F80)
)

private val LightColorScheme = lightColorScheme(
    primary = OfficeBluePrimary,
    onPrimary = Color.White,
    primaryContainer = OfficeBlueContainer,
    onPrimaryContainer = OnOfficeBlueContainer,
    secondary = ExcelGreen,
    onSecondary = Color.White,
    secondaryContainer = ExcelGreenContainer,
    onSecondaryContainer = Color(0xFF04341B),
    tertiary = PowerPointOrange,
    onTertiary = Color.White,
    tertiaryContainer = PowerPointOrangeContainer,
    onTertiaryContainer = Color(0xFF4E1400),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded office colors by default for consistent suite identity
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
