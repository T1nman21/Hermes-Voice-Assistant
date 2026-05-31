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
    var roomCode by remember { mutableStateOf("") }
    var relayAddress by remember { mutableStateOf("") }
    var relayPort by remember { mutableStateOf("8643") }

    val fullRelayUrl = if (relayAddress.isNotBlank()) {
        "ws://$relayAddress:${relayPort.ifBlank { "8643" }}"
    } else ""

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
                            FeatureRow(Icons.Default.Wifi, "Relay Connected", "Pair with a simple room code — no port forwarding")
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
                    // Relay + room code entry
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Pair with Your Desktop", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text(
                            "Start the relay on your desktop first.\nThen enter the room code shown in the terminal.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Relay address (optional — auto-filled to desktop IP)
                        OutlinedTextField(
                            value = relayAddress,
                            onValueChange = { relayAddress = it },
                            label = { Text("Desktop Address", color = OpenRockyPalette.muted) },
                            placeholder = { Text("192.168.1.100", color = OpenRockyPalette.label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Computer, null, tint = OpenRockyPalette.accent)
                            }
                        )

                        // Room code (4-6 chars, auto-capitalized)
                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                            label = { Text("Room Code", color = OpenRockyPalette.muted) },
                            placeholder = { Text("ABCD", color = OpenRockyPalette.label) },
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
                            "On your desktop, run:\nnode relay/desktop-client.js --room $roomCode",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.label,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        val canConnect = relayAddress.isNotBlank() && roomCode.length >= 3
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

                        // Pass relay URL + room code as a composite "key"
                        val relayKey = "$fullRelayUrl|$roomCode"

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
