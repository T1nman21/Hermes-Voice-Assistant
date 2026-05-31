//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.theme

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

private val FallbackDarkScheme = darkColorScheme(
    primary = OpenRockyPrimary,
    secondary = OpenRockySecondary,
    background = OpenRockyDarkBackground,
    surface = OpenRockyDarkSurface,
    surfaceVariant = OpenRockyDarkSurfaceVariant,
    onPrimary = Color.White,
    onBackground = Color(0xE0FFFFFF),
    onSurface = Color(0xE0FFFFFF),
    onSurfaceVariant = Color(0x8CFFFFFF),
    outline = Color(0x1AFFFFFF),
    error = OpenRockyError,
    onError = Color.White,
    surfaceContainer = OpenRockyDarkSurface,
    surfaceContainerHigh = OpenRockyDarkSurfaceVariant,
)

private val FallbackLightScheme = lightColorScheme(
    primary = OpenRockyPrimary,
    secondary = OpenRockySecondary,
    background = OpenRockyLightBackground,
    surface = OpenRockyLightSurface,
    surfaceVariant = OpenRockyLightSurfaceVariant,
    onPrimary = Color.White,
    onBackground = Color(0xE0000000),
    onSurface = Color(0xE0000000),
    onSurfaceVariant = Color(0x80000000),
    outline = Color(0x1A000000),
    error = OpenRockyError,
    onError = Color.White,
    surfaceContainer = OpenRockyLightSurface,
    surfaceContainerHigh = OpenRockyLightSurfaceVariant,
)

/**
 * Theme entry point.
 *
 * On Android 12+ ([Build.VERSION_CODES.S]), when [dynamicColor] is true (default), the color
 * scheme is derived from the user's current wallpaper (Material You). On older devices or when
 * disabled, the fixed Rocky brand scheme is used instead.
 *
 * This makes Rocky feel like a native Android 12+ citizen — cross-app theming iOS cannot offer.
 */
@Composable
fun OpenRockyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkScheme
        else -> FallbackLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenRockyTypography,
        content = content
    )
}
