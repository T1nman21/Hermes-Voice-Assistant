//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.ui.theme.OpenRockyPalette

@Composable
fun TopChromeView(
    isVoiceActive: Boolean,
    onSettingsClick: () -> Unit,
    onVoiceToggle: () -> Unit,
    onNewChatClick: () -> Unit,
    onConversationsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(OpenRockyPalette.background, OpenRockyPalette.background.copy(alpha = 0f))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left cluster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, "Settings", tint = OpenRockyPalette.muted)
                }
                Text(
                    "Rocky",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenRockyPalette.text
                )
            }

            // Right cluster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onConversationsClick != null) {
                    IconButton(onClick = onConversationsClick) {
                        Icon(Icons.Default.Forum, "Conversations", tint = OpenRockyPalette.muted)
                    }
                }
                IconButton(onClick = onNewChatClick) {
                    Icon(Icons.Default.Add, "New Chat", tint = OpenRockyPalette.muted)
                }

                // Voice button
                IconButton(
                    onClick = onVoiceToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isVoiceActive) OpenRockyPalette.accent.copy(alpha = pulseAlpha * 0.2f)
                            else Color.Transparent
                        )
                ) {
                    Icon(
                        imageVector = if (isVoiceActive) Icons.Default.Stop else Icons.Default.GraphicEq,
                        contentDescription = if (isVoiceActive) "Stop Voice" else "Start Voice",
                        tint = if (isVoiceActive) OpenRockyPalette.accent else OpenRockyPalette.muted
                    )
                }
            }
        }
    }
}
