//
// Hermes Voice — Onboarding
// Modified from OpenRocky's OnboardingView
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.ui.theme.OpenRockyPalette

/**
 * Hermes Voice onboarding — connect to your desktop Hermes agent
 * instead of entering an API key.
 *
 * Flow:
 *  1. Welcome → 2. Enter desktop address → 3. Connected!
 */
@Composable
fun OnboardingView(
    onComplete: (String) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var desktopAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8642") }

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
                    // Welcome — rebranded for Hermes Voice
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

                        Text("Hermes Voice", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text, textAlign = TextAlign.Center)
                        Text("Your desktop AI agent, now voice-activated on your phone", fontSize = 16.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                        Spacer(Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureRow(Icons.Default.Mic, "Wake Word", "Say \"Hey Hermes\" to activate")
                            FeatureRow(Icons.Default.Computer, "Desktop Powered", "Runs on your home PC, not the cloud")
                            FeatureRow(Icons.Default.RecordVoiceOver, "Voice-First", "Talk naturally and get spoken responses")
                            FeatureRow(Icons.Default.Wifi, "Wi-Fi Connected", "Works on your home network")
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Connect to Desktop", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        TextButton(onClick = onSkip) {
                            Text("Configure later", color = OpenRockyPalette.muted)
                        }
                    }
                }
                1 -> {
                    // Desktop address entry — no API key needed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Connect to Your Desktop", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text(
                            "Enter the IP address of the computer running Hermes Agent.\nBoth devices must be on the same Wi-Fi network.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // Desktop IP / hostname
                        OutlinedTextField(
                            value = desktopAddress,
                            onValueChange = { desktopAddress = it },
                            label = { Text("Desktop Address", color = OpenRockyPalette.muted) },
                            placeholder = { Text("192.168.1.100 or my-pc.local", color = OpenRockyPalette.label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Computer, null, tint = OpenRockyPalette.accent)
                            }
                        )

                        // Port (default 8642 for Hermes API)
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it.filter { c -> c.isDigit() } },
                            label = { Text("Port", color = OpenRockyPalette.muted) },
                            placeholder = { Text("8642", color = OpenRockyPalette.label) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = rockyTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.SettingsEthernet, null, tint = OpenRockyPalette.accent)
                            }
                        )

                        // Show the full URL that will be used
                        val previewUrl = if (desktopAddress.isNotBlank()) {
                            "http://$desktopAddress:${port.ifBlank { "8642" }}/v1"
                        } else ""
                        if (previewUrl.isNotBlank()) {
                            Text(
                                "Will connect to: $previewUrl",
                                fontSize = 12.sp,
                                color = OpenRockyPalette.muted,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Tip: Run \"ipconfig\" on your Windows PC to find its IP address.",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.label,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { step = 2 },
                            enabled = desktopAddress.isNotBlank(),
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
                    // Connected successfully
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

                        Text("Connected!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Text(
                            "Hermes Voice is ready.\nTap the mic to start talking to your desktop agent.",
                            fontSize = 16.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        // Build the Hermes connection URL and pass it as the "API key"
                        // The ViewModel will parse this into a Hermes provider config
                        val host = "http://$desktopAddress:${port.ifBlank { "8642" }}/v1"

                        Button(
                            onClick = { onComplete(host) },
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
