//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
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

    // ── Static variants for non-composable contexts (enums, data classes) ──
    val mutedStatic = Color(0x8CFFFFFF)
    val labelStatic = Color(0x66FFFFFF)

    // ── Brand colors (static fallback, used in non-composable contexts like enum properties) ──
    val accentBrand = Color(0xFF4AC7E3)
    val secondaryBrand = Color(0xFFFB8C57)

    // ── Voice-mode tint (deep blue, matches iOS post-2026-04 voice palette) ──
    val voicePrimary = Color(0xFF4D8FE0)
    val voiceDeep = Color(0xFF1E3A7A)

    // ── Semantic (same in both modes; not affected by dynamic color) ──
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

    /**
     * Accent color. On Android 12+ with dynamic color enabled, this reflects the user's
     * wallpaper (Material You). Falls back to the brand cyan otherwise.
     *
     * [MaterialTheme.colorScheme.primary] is hooked up to the dynamic scheme inside [OpenRockyTheme]
     * when running on API 31+; on older devices it resolves to the fixed [accentBrand].
     */
    val accent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary

    /**
     * Secondary accent. Follows dynamic color on Android 12+, falls back to brand orange.
     */
    val secondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
}
