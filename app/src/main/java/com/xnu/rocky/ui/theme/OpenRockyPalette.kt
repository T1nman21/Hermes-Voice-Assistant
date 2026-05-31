//
// Hermes Voice — brand palette
// Based on OpenRocky's palette system, with unique Hermes colors
//

package com.xnu.rocky.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object OpenRockyPalette {
    // ── Dark mode colors ──
    private val darkBackground = Color(0xFF0F1420)
    private val darkCard = Color(0xFF1A1F2B)
    private val darkCardElevated = Color(0xFF212838)
    private val darkCardPressed = Color(0xFF141824)
    private val darkStroke = Color(0x1AFFFFFF)
    private val darkStrokeSubtle = Color(0x0FFFFFFF)
    private val darkSeparator = Color(0x14FFFFFF)
    private val darkText = Color(0xE0FFFFFF)
    private val darkMuted = Color(0x8CFFFFFF)
    private val darkLabel = Color(0x66FFFFFF)

    // ── Light mode colors ──
    private val lightBackground = Color(0xFFF5F5FA)
    private val lightCard = Color(0xFFFFFFFF)
    private val lightCardElevated = Color(0xFFEDEDF2)
    private val lightCardPressed = Color(0xFFE6E6EB)
    private val lightStroke = Color(0x1A000000)
    private val lightStrokeSubtle = Color(0x0F000000)
    private val lightSeparator = Color(0x14000000)
    private val lightText = Color(0xE0000000)
    private val lightMuted = Color(0x80000000)
    private val lightLabel = Color(0x59000000)

    // ── Static variants for non-composable contexts ──
    val mutedStatic = Color(0x8CFFFFFF)
    val labelStatic = Color(0x66FFFFFF)

    // ── Hermes brand colors (purple + amber, distinct from OpenRocky's cyan/orange) ──
    val accentBrand = Color(0xFF7C3AED)    // Deep violet — primary accent
    val secondaryBrand = Color(0xFFF59E0B)  // Amber gold — secondary accent

    // ── Voice-mode tint (purple gradient) ──
    val voicePrimary = Color(0xFF8B5CF6)    // Lighter violet
    val voiceDeep = Color(0xFF4C1D95)       // Deep indigo

    // ── Semantic colors ──
    val success = Color(0xFF6EE39E)
    val warning = Color(0xFFFABF59)
    val error = Color(0xFFE35D6A)
    val shadow = Color(0x52000000)

    // ── Adaptive accessors ──
    val background: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkBackground else lightBackground
    val card: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkCard else lightCard
    val cardElevated: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkCardElevated else lightCardElevated
    val cardPressed: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkCardPressed else lightCardPressed
    val stroke: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkStroke else lightStroke
    val strokeSubtle: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkStrokeSubtle else lightStrokeSubtle
    val separator: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkSeparator else lightSeparator
    val text: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkText else lightText
    val muted: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkMuted else lightMuted
    val label: Color @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkLabel else lightLabel

    val accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val secondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
}
