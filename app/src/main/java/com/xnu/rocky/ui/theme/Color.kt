//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.theme

import androidx.compose.ui.graphics.Color

// Static brand colors used to seed the fallback Material color scheme (non-dynamic devices).
// Dynamic-color theming is wired up in Theme.kt on API 31+.
val OpenRockyPrimary = OpenRockyPalette.accentBrand
val OpenRockySecondary = OpenRockyPalette.secondaryBrand
val OpenRockyError = OpenRockyPalette.error

// Dark mode surface colors (default)
val OpenRockyDarkBackground = Color(0xFF0F1420)
val OpenRockyDarkSurface = Color(0xFF1A1F2B)
val OpenRockyDarkSurfaceVariant = Color(0xFF212838)

// Light mode surface colors
val OpenRockyLightBackground = Color(0xFFF5F5FA)
val OpenRockyLightSurface = Color(0xFFFFFFFF)
val OpenRockyLightSurfaceVariant = Color(0xFFEDEDF2)
