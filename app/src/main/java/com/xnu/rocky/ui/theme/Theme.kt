//
// Hermes Voice — Theme (forked from OpenRocky)
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

// Hermes purple palette (replaces Rocky cyan/orange)
private val HermesPrimary = OpenRockyPalette.accentBrand        // #7C3AED deep violet
private val HermesSecondary = OpenRockyPalette.secondaryBrand   // #F59E0B amber gold
private val HermesError = OpenRockyPalette.error

private val HermesDarkBg = Color(0xFF0F1420)
private val HermesDarkSurface = Color(0xFF1A1F2B)
private val HermesDarkSurfaceVariant = Color(0xFF212838)

private val HermesLightBg = Color(0xFFF5F5FA)
private val HermesLightSurface = Color(0xFFFFFFFF)
private val HermesLightSurfaceVariant = Color(0xFFEDEDF2)

private val FallbackDarkScheme = darkColorScheme(
    primary = HermesPrimary,
    secondary = HermesSecondary,
    background = HermesDarkBg,
    surface = HermesDarkSurface,
    surfaceVariant = HermesDarkSurfaceVariant,
    onPrimary = Color.White,
    onBackground = Color(0xE0FFFFFF),
    onSurface = Color(0xE0FFFFFF),
    onSurfaceVariant = Color(0x8CFFFFFF),
    outline = Color(0x1AFFFFFF),
    error = HermesError,
    onError = Color.White,
    surfaceContainer = HermesDarkSurface,
    surfaceContainerHigh = HermesDarkSurfaceVariant,
)

private val FallbackLightScheme = lightColorScheme(
    primary = HermesPrimary,
    secondary = HermesSecondary,
    background = HermesLightBg,
    surface = HermesLightSurface,
    surfaceVariant = HermesLightSurfaceVariant,
    onPrimary = Color.White,
    onBackground = Color(0xE0000000),
    onSurface = Color(0xE0000000),
    onSurfaceVariant = Color(0x80000000),
    outline = Color(0x1A000000),
    error = HermesError,
    onError = Color.White,
    surfaceContainer = HermesLightSurface,
    surfaceContainerHigh = HermesLightSurfaceVariant,
)

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
