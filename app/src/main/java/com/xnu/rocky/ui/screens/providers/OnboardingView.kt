//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.ui.theme.OpenRockyPalette

@Composable
fun OnboardingView(
    onComplete: (String) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenRockyPalette.background)
            .systemBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = step, label = "onboarding") { currentStep ->
            when (currentStep) {
                0 -> {
                    // Welcome
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(OpenRockyPalette.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(40.dp))
                        }

                        Text("Welcome to Rocky", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text, textAlign = TextAlign.Center)
                        Text("Your voice-first AI agent for Android", fontSize = 16.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureRow(Icons.Default.GraphicEq, "Voice-First", "Talk naturally with real-time voice")
                            FeatureRow(Icons.Default.Build, "Native Tools", "Calendar, contacts, weather, and more")
                            FeatureRow(Icons.Default.AutoAwesome, "Smart Skills", "Custom skills for any workflow")
                            FeatureRow(Icons.Default.Memory, "Persistent Memory", "Rocky remembers across sessions")
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Get Started", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        TextButton(onClick = onSkip) {
                            Text("Skip for now", color = OpenRockyPalette.muted)
                        }
                    }
                }
                1 -> {
                    // API Key entry
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Connect OpenAI", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text("Enter your OpenAI API key to get started.\nThis enables both chat and voice.", fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKey, onValueChange = { apiKey = it },
                            label = { Text("API Key", color = OpenRockyPalette.muted) },
                            placeholder = { Text("sk-...", color = OpenRockyPalette.label) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { step = 2 },
                            enabled = apiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Continue", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        TextButton(onClick = onSkip) {
                            Text("Configure later", color = OpenRockyPalette.muted)
                        }
                    }
                }
                2 -> {
                    // Success
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(OpenRockyPalette.success.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = OpenRockyPalette.success, modifier = Modifier.size(40.dp))
                        }

                        Text("You're all set!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text("Rocky is ready to help you.", fontSize = 16.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { onComplete(apiKey) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start Using Rocky", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(24.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
            Text(subtitle, fontSize = 12.sp, color = OpenRockyPalette.muted)
        }
    }
}
