//
// Hermes Voice — Onboarding with room code pairing
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    var relayUrl by remember { mutableStateOf("wss://hospitality-musicians-hunting-wedding.trycloudflare.com") }
    var roomCode by remember { mutableStateOf("HERM") }

    val canConnect = relayUrl.isNotBlank() && roomCode.length >= 3

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
                            Icon(Icons.Default.Mic, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(40.dp))
                        }

                        Text("Hermes Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text, textAlign = TextAlign.Center)
                        Text("Your desktop AI agent, now voice-activated on your phone", fontSize = 16.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureRow(Icons.Default.Mic, "Wake Word", "Say \"Hey Hermes\" to activate")
                            FeatureRow(Icons.Default.Computer, "Desktop Powered", "Runs on your home PC, not the cloud")
                            FeatureRow(Icons.Default.RecordVoiceOver, "Voice-First", "Talk naturally and get spoken responses")
                            FeatureRow(Icons.Default.Wifi, "Relay Connected", "Pair with a room code — works from anywhere via Cloudflare")
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Pair with Desktop", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        TextButton(onClick = onSkip) {
                            Text("Configure later", color = OpenRockyPalette.muted)
                        }
                    }
                }
                1 -> {
                    // Relay URL + room code entry
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Connect via Relay", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text(
                            "Run start-relay.bat on your desktop.\nPaste the URL shown in the terminal below.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Relay URL (Cloudflare tunnel or local)
                        OutlinedTextField(
                            value = relayUrl,
                            onValueChange = { relayUrl = it },
                            label = { Text("Relay URL", color = OpenRockyPalette.muted) },
                            placeholder = { Text("wss://xxx.trycloudflare.com", color = OpenRockyPalette.label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, null, tint = OpenRockyPalette.accent)
                            }
                        )

                        // Room code
                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                            label = { Text("Room Code", color = OpenRockyPalette.muted) },
                            placeholder = { Text("HERM", color = OpenRockyPalette.label) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Characters
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Key, null, tint = OpenRockyPalette.accent)
                            }
                        )

                        Text(
                            "On desktop: run start-relay.bat\nOn phone: paste the URL above",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.label,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { step = 2 },
                            enabled = canConnect,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Connect", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        TextButton(onClick = onSkip) {
                            Text("I'll set this up later", color = OpenRockyPalette.muted)
                        }
                    }
                }
                2 -> {
                    // Connected
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

                        Text("Paired!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text(
                            "Phone and desktop are connected through the relay.\nTap the mic to start talking to Hermes.",
                            fontSize = 16.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        // Use default tunnel URL if none entered
                        val effectiveUrl = relayUrl.ifBlank {
                            "wss://hospitality-musicians-hunting-wedding.trycloudflare.com"
                        }
                        val relayKey = "$effectiveUrl|$roomCode"

                        Button(
                            onClick = { onComplete(relayKey) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Start Using Hermes Voice", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
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
