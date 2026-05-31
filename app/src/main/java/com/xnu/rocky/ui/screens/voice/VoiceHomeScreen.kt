//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import com.xnu.rocky.models.OpenRockySession
import com.xnu.rocky.models.SessionMode
import com.xnu.rocky.runtime.ConversationMessage
import com.xnu.rocky.runtime.DelegateProgress
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.delay

private val placeholderUserTexts = setOf(
    "Speak or type a request to start a real session."
)
private val placeholderAssistantTexts = setOf(
    "Rocky is waiting for a model reply.",
    "Rocky is idle. Start voice or send text to attach a live runtime."
)

// Rocky-specific tips: each one maps to a distinct strength so the home screen
// advertises depth (focus timer / Health Connect / weather+location / reminders /
// calendar / delegate-task planning) instead of generic-AI-chatbot prompts.
// Mirrors iOS OpenRockyVoiceHomeView.tips.
private val tips = listOf(
    "Try \"Set a 25-minute focus timer\"",
    "Try \"How did I sleep last night?\"",
    "Try \"Will it rain when I head home?\"",
    "Try \"Add bread and milk to my reminders\"",
    "Try \"What's on my calendar tomorrow?\"",
    "Try \"Plan a weekend trip to Hangzhou\""
)

private data class TranscriptPair(val id: String, val userText: String?, val assistantText: String?)

@Composable
fun VoiceHomeScreen(
    session: OpenRockySession,
    isVoiceActive: Boolean,
    realtimeConfigured: Boolean,
    providerLabel: String,
    recentMessages: List<ConversationMessage>,
    /**
     * Live snapshot of the in-flight delegate-task. When non-null the
     * [DelegateProgressPanel] floats above the orb so the user can see what
     * the chat sub-agent is actually doing during a 5–30s delegation, instead
     * of staring at a single status line. Mirrors iOS overlay panel.
     */
    liveDelegateProgress: DelegateProgress?,
    onOpenSettings: () -> Unit,
    onOpenChat: () -> Unit,
    onToggleVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showsNotConfiguredAlert by remember { mutableStateOf(false) }
    var tipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4_500)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    val modeTint = when (session.mode) {
        SessionMode.LISTENING, SessionMode.READY -> OpenRockyPalette.voicePrimary
        SessionMode.PLANNING, SessionMode.EXECUTING -> OpenRockyPalette.voiceDeep
    }

    val liveUserText = session.liveTranscript.takeIf { it.isNotEmpty() && it !in placeholderUserTexts }
    val liveAssistantText = session.assistantReply.takeIf { it.isNotEmpty() && it !in placeholderAssistantTexts }

    // Group persisted messages into (user, assistant) pairs and drop the live turn.
    val recentPairs = remember(recentMessages) {
        val pairs = mutableListOf<TranscriptPair>()
        var i = 0
        while (i < recentMessages.size) {
            val msg = recentMessages[i]
            if (msg.role == "user") {
                val assistantText = if (i + 1 < recentMessages.size && recentMessages[i + 1].role == "assistant") {
                    i += 2
                    recentMessages[i - 1].content
                } else {
                    i += 1
                    null
                }
                pairs.add(TranscriptPair(id = msg.id, userText = msg.content, assistantText = assistantText))
            } else {
                i += 1
            }
        }
        pairs.dropLast(1).takeLast(3)
    }

    val hasAnyTranscript = liveUserText != null || liveAssistantText != null || recentPairs.isNotEmpty()

    Box(modifier = modifier
        .fillMaxSize()
        .background(OpenRockyPalette.background)
    ) {
        AmbientBackground(modeTint = modeTint, micActive = isVoiceActive)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            Column(modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
            ) {
                TopBar(
                    providerLabel = providerLabel,
                    isVoiceReady = realtimeConfigured,
                    onOpenSettings = onOpenSettings,
                    onOpenChat = onOpenChat
                )
                Spacer(Modifier.height(6.dp))

                // Orb is always anchored at the bottom (compact) or to the leading
                // edge (wide). The space above is the info surface: transcript when
                // a turn is in flight or recent history exists, otherwise a quiet
                // greeting + rotating tip card. Anchoring the orb avoids the vertical
                // jump users used to see on the first tap.
                val triggerVoice: () -> Unit = {
                    if (!isVoiceActive && !realtimeConfigured) {
                        showsNotConfiguredAlert = true
                    } else {
                        onToggleVoice()
                    }
                }
                if (isWide) {
                    Row(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        VoiceCanvas(
                            session = session,
                            isMicActive = isVoiceActive,
                            modeTint = modeTint,
                            onTriggerVoice = triggerVoice,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (hasAnyTranscript) {
                                TranscriptFeed(
                                    pairs = recentPairs,
                                    liveUserText = liveUserText,
                                    liveAssistantText = liveAssistantText,
                                    modeTint = modeTint,
                                    modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)
                                )
                            } else {
                                IdleHeader(
                                    tipIndex = tipIndex,
                                    modeTint = modeTint,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (hasAnyTranscript) {
                            TranscriptFeed(
                                pairs = recentPairs,
                                liveUserText = liveUserText,
                                liveAssistantText = liveAssistantText,
                                modeTint = modeTint,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            IdleHeader(
                                tipIndex = tipIndex,
                                modeTint = modeTint,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    VoiceCanvas(
                        session = session,
                        isMicActive = isVoiceActive,
                        modeTint = modeTint,
                        onTriggerVoice = triggerVoice,
                        modifier = Modifier.padding(bottom = 28.dp)
                    )
                }
            }
        }

        // Live delegate-task progress overlay. Floats above the orb so the
        // user can see what the chat sub-agent is doing during a 5–30s
        // delegation, instead of staring at a single status line. Animates
        // in/out via DelegateProgressPanel itself.
        DelegateProgressPanel(
            progress = liveDelegateProgress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 200.dp)
                .navigationBarsPadding()
        )
    }

    if (showsNotConfiguredAlert) {
        AlertDialog(
            onDismissRequest = { showsNotConfiguredAlert = false },
            title = { Text("Voice Not Configured") },
            text = { Text("Please set up a voice provider in Settings before starting a voice session.") },
            confirmButton = {
                TextButton(onClick = {
                    showsNotConfiguredAlert = false
                    onOpenSettings()
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showsNotConfiguredAlert = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AmbientBackground(modeTint: Color, micActive: Boolean) {
    val haloAlpha by animateFloatAsState(
        targetValue = if (micActive) 0.30f else 0.16f,
        animationSpec = tween(900),
        label = "haloAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(modeTint.copy(alpha = haloAlpha), Color.Transparent),
                    center = Offset(x = 540f, y = 0f),
                    radius = 1400f
                )
            )
    )
}

@Composable
private fun TopBar(
    providerLabel: String,
    isVoiceReady: Boolean,
    onOpenSettings: () -> Unit,
    onOpenChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(OpenRockyPalette.cardElevated)
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = OpenRockyPalette.muted, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.weight(1f))

        Surface(
            onClick = onOpenSettings,
            color = OpenRockyPalette.cardElevated,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isVoiceReady) OpenRockyPalette.success else OpenRockyPalette.warning)
                )
                Text(
                    providerLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpenRockyPalette.text.copy(alpha = 0.82f),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onOpenChat,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(OpenRockyPalette.cardElevated)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, "Chat", tint = OpenRockyPalette.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun VoiceCanvas(
    session: OpenRockySession,
    isMicActive: Boolean,
    modeTint: Color,
    onTriggerVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            PulseRings(modeTint = modeTint, micActive = isMicActive)

            // Halo
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .blur(14.dp)
                    .clip(CircleShape)
                    .background(modeTint.copy(alpha = 0.18f))
            )

            // Main orb button
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                modeTint.copy(alpha = 0.97f),
                                modeTint.copy(alpha = 0.42f),
                                OpenRockyPalette.card
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onTriggerVoice,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = orbIcon(session.mode, isMicActive),
                        contentDescription = stateLabel(session.mode, isMicActive),
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }

        StatusPill(modeTint = modeTint, mode = session.mode, isMicActive = isMicActive)

        // Constant-height container only confirms the mic is hot. The idle tip
        // lives in IdleHeader now so the canvas itself never resizes.
        Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            if (isMicActive && session.mode == SessionMode.LISTENING) {
                LiveWaveform(modeTint = modeTint)
            }
        }
    }
}

/// Sits in the otherwise-empty space above the anchored orb. Calm, glanceable,
/// and replaced by the transcript feed the moment a turn starts so the orb
/// itself never moves.
@Composable
private fun IdleHeader(
    tipIndex: Int,
    modeTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                greeting(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OpenRockyPalette.text.copy(alpha = 0.92f)
            )
            Text(
                "How can I help today?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OpenRockyPalette.muted
            )
        }
        Spacer(Modifier.height(14.dp))
        RotatingTipCard(tipIndex = tipIndex, modeTint = modeTint)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RotatingTipCard(tipIndex: Int, modeTint: Color) {
    Surface(
        color = OpenRockyPalette.cardElevated.copy(alpha = 0.7f),
        shape = RoundedCornerShape(percent = 50),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, OpenRockyPalette.stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = modeTint.copy(alpha = 0.9f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                tips[tipIndex % tips.size],
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OpenRockyPalette.text.copy(alpha = 0.82f),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun PulseRings(modeTint: Color, micActive: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulseRings")
    val phaseSpeed = if (micActive) 1818 else 3125  // ms for one cycle (faster when active)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(phaseSpeed, easing = LinearEasing)
        ),
        label = "phase"
    )

    val amp = if (micActive) 0.50f else 0.26f
    val opacityAmp = if (micActive) 0.55f else 0.30f
    val strokeWidth = if (micActive) 1.6f else 1.1f

    Canvas(modifier = Modifier.size(240.dp)) {
        val baseRadius = size.minDimension / 2 * 0.74f
        for (i in 0 until 3) {
            val ringPhase = ((phase + i / 3f) % 1f)
            val r = baseRadius * (1f + ringPhase * amp)
            val a = (1f - ringPhase) * opacityAmp
            drawCircle(
                color = modeTint.copy(alpha = a),
                radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.dp.toPx())
            )
        }
    }
}

@Composable
private fun StatusPill(modeTint: Color, mode: SessionMode, isMicActive: Boolean) {
    val label = stateLabel(mode, isMicActive)
    val dotColor = stateDotColor(mode, isMicActive, modeTint)
    Surface(
        color = OpenRockyPalette.cardElevated,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isMicActive) 1f else 0.6f))
            )
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpenRockyPalette.text
            )
        }
    }
}

@Composable
private fun LiveWaveform(modeTint: Color) {
    val transition = rememberInfiniteTransition(label = "wave")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
        label = "wavetick"
    )
    Row(
        modifier = Modifier.height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        for (i in 0 until 7) {
            val phase = tick * (2 * Math.PI).toFloat() + i * 0.85f
            val normalized = (kotlin.math.sin(phase) * 0.5f + 0.5f)
            val height = 6f + normalized * 22f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(modeTint.copy(alpha = 0.9f))
            )
        }
    }
}

@Composable
private fun TranscriptFeed(
    pairs: List<TranscriptPair>,
    liveUserText: String?,
    liveAssistantText: String?,
    modeTint: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val total = (pairs.size + (if (liveUserText != null) 1 else 0) + (if (liveAssistantText != null) 1 else 0))
    LaunchedEffect(total, liveUserText, liveAssistantText) {
        if (total > 0) listState.animateScrollToItem(total - 1)
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        pairs.forEachIndexed { idx, pair ->
            val freshness = 0.55f + (idx.toFloat() / pairs.size.coerceAtLeast(1)) * 0.30f
            if (!pair.userText.isNullOrEmpty()) {
                item(key = "${pair.id}-u") { UserBubble(pair.userText, freshness, modeTint) }
            }
            if (!pair.assistantText.isNullOrEmpty()) {
                item(key = "${pair.id}-a") { AssistantBubble(pair.assistantText, freshness) }
            }
        }
        if (liveUserText != null) item(key = "live-u") { UserBubble(liveUserText, 1f, modeTint) }
        if (liveAssistantText != null) item(key = "live-a") { AssistantBubble(liveAssistantText, 1f) }
    }
}

@Composable
private fun UserBubble(text: String, freshness: Float, modeTint: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Spacer(Modifier.width(48.dp))
        Surface(
            color = modeTint.copy(alpha = (0.55f * freshness + 0.18f).coerceIn(0f, 1f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = (freshness + 0.05f).coerceIn(0f, 1f))
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String, freshness: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = OpenRockyPalette.cardElevated.copy(alpha = (0.55f * freshness + 0.20f).coerceIn(0f, 1f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OpenRockyPalette.text.copy(alpha = freshness.coerceIn(0f, 1f))
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

private fun orbIcon(mode: SessionMode, isMicActive: Boolean) = if (isMicActive) {
    when (mode) {
        SessionMode.LISTENING -> Icons.Default.GraphicEq
        SessionMode.PLANNING -> Icons.Default.MoreHoriz
        SessionMode.EXECUTING -> Icons.Default.Bolt
        SessionMode.READY -> Icons.Default.GraphicEq
    }
} else {
    Icons.Default.Mic
}

private fun stateLabel(mode: SessionMode, isMicActive: Boolean): String {
    if (isMicActive) {
        return when (mode) {
            SessionMode.LISTENING -> "Listening"
            SessionMode.PLANNING -> "Thinking"
            SessionMode.EXECUTING -> "Responding"
            SessionMode.READY -> "Ready"
        }
    }
    if (mode == SessionMode.PLANNING) return "Connecting…"
    return "Tap to talk"
}

@Composable
private fun stateDotColor(mode: SessionMode, isMicActive: Boolean, modeTint: Color): Color {
    if (!isMicActive) {
        return if (mode == SessionMode.PLANNING) OpenRockyPalette.warning else OpenRockyPalette.muted
    }
    return when (mode) {
        SessionMode.LISTENING -> OpenRockyPalette.success
        SessionMode.PLANNING -> OpenRockyPalette.warning
        SessionMode.EXECUTING -> modeTint
        SessionMode.READY -> OpenRockyPalette.muted
    }
}
