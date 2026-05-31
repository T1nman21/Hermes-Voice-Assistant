//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.ui.theme.OpenRockyPalette

/** Pure-text chat composer. Voice input is the dedicated Realtime path elsewhere; the system keyboard's
 *  built-in dictation handles speech-to-text on the composer if the user wants it. */
@Composable
fun ComposerBarStandalone(
    onSendMessage: (String) -> Unit,
    onConversationsClick: () -> Unit = {},
    minimalistLayout: Boolean = false
) {
    var text by remember { mutableStateOf("") }

    Surface(color = OpenRockyPalette.card, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!minimalistLayout) {
                IconButton(onClick = onConversationsClick, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Forum, "Conversations", tint = OpenRockyPalette.muted, modifier = Modifier.size(22.dp))
                }
            }

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(OpenRockyPalette.cardElevated)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                textStyle = LocalTextStyle.current.copy(color = OpenRockyPalette.text, fontSize = 15.sp),
                cursorBrush = SolidColor(OpenRockyPalette.accent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text("Ask Rocky anything…", color = OpenRockyPalette.label, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (text.isNotBlank()) OpenRockyPalette.accent else OpenRockyPalette.cardElevated)
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) OpenRockyPalette.background else OpenRockyPalette.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
