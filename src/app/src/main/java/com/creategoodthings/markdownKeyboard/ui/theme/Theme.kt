package com.creategoodthings.markdownKeyboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Deliberately not a dynamic (Material You) scheme. The keyboard is drawn on top of another
 * app, so a wallpaper-derived accent would land under all thirty-odd keys with nothing to
 * check it against; the companion app then matches the keyboard rather than the wallpaper.
 */
private val LightColorScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccentLight,
    primaryContainer = AccentLightContainer,
    onPrimaryContainer = OnAccentLightContainer,
    secondary = NeutralLight30,
    onSecondary = KeyWhite,
    secondaryContainer = NeutralLight92,
    onSecondaryContainer = NeutralLight10,
    tertiary = AccentLight,
    onTertiary = OnAccentLight,
    background = NeutralLight99,
    onBackground = NeutralLight10,
    surface = NeutralLight99,
    onSurface = NeutralLight10,
    surfaceVariant = NeutralLight92,
    onSurfaceVariant = NeutralLight30,
    surfaceContainerLowest = KeyWhite,
    surfaceContainerLow = NeutralLight99,
    surfaceContainer = NeutralLight95,
    surfaceContainerHigh = NeutralLight92,
    surfaceContainerHighest = NeutralLight88,
    outline = NeutralLight30,
    outlineVariant = NeutralLight80,
    surfaceTint = AccentLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentDarkContainer,
    onPrimaryContainer = OnAccentDarkContainer,
    secondary = NeutralDark80,
    onSecondary = NeutralDark12,
    secondaryContainer = NeutralDark22,
    onSecondaryContainer = NeutralDark90,
    tertiary = AccentDark,
    onTertiary = OnAccentDark,
    background = NeutralDark06,
    onBackground = NeutralDark90,
    surface = NeutralDark06,
    onSurface = NeutralDark90,
    surfaceVariant = NeutralDark22,
    onSurfaceVariant = NeutralDark80,
    surfaceContainerLowest = NeutralDark06,
    surfaceContainerLow = NeutralDark08,
    surfaceContainer = NeutralDark12,
    surfaceContainerHigh = NeutralDark14,
    surfaceContainerHighest = NeutralDark22,
    outline = NeutralDark60,
    outlineVariant = NeutralDark22,
    surfaceTint = AccentDark,
)

@Composable
fun MarkdownKeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val keyboardColors = if (darkTheme) DarkKeyboardColors else LightKeyboardColors

    CompositionLocalProvider(LocalKeyboardColors provides keyboardColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
