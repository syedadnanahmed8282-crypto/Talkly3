package com.family.talkly.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkPurple,
    secondary = SecondaryLightSage,
    tertiary = SecondaryLightSage,
    background = WhatsappDarkBg,
    surface = PrimaryDarkPurple,
    onPrimary = Color.White,
    onSecondary = PrimaryDarkPurple,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = PrimaryDarkPurple,
    secondaryContainer = SecondaryLightSage
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDarkPurple,
    secondary = SecondaryLightSage,
    tertiary = SecondaryLightSage,
    background = Color(0xFFFAFCFA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = PrimaryDarkPurple,
    onBackground = Color(0xFF1E1025),
    onSurface = Color(0xFF1E1025),
    primaryContainer = PrimaryDarkPurple,
    secondaryContainer = SecondaryLightSage
)

@Composable
fun TalklyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PrimaryDarkPurple.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
