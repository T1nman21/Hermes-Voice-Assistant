//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.runtime.voice

enum class OpenAIVoice(val displayName: String, val subtitle: String) {
    ALLOY("Alloy", "Neutral and balanced"),
    ASH("Ash", "Warm and engaging"),
    BALLAD("Ballad", "Soft and gentle"),
    CORAL("Coral", "Clear and bright"),
    ECHO("Echo", "Smooth and composed"),
    SAGE("Sage", "Calm and measured"),
    SHIMMER("Shimmer", "Bright and expressive"),
    VERSE("Verse", "Rich and resonant");

    val id: String get() = name.lowercase()
}
