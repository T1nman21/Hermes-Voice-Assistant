package com.xnu.rocky

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xnu.rocky.hermes.HermesVoiceSession
import com.xnu.rocky.ui.theme.OpenRockyPalette
import com.xnu.rocky.ui.theme.OpenRockyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenRockyTheme {
                HermesMainApp()
            }
        }
    }
}

@Composable
fun HermesMainApp() {
    val viewModel: OpenRockyViewModel = viewModel()
    val context = LocalContext.current
    val hermesState by viewModel.hermesVoiceSession.state.collectAsStateWithLifecycle()
    val hermesStatus by viewModel.hermesVoiceSession.statusText.collectAsStateWithLifecycle()
    val hermesTranscript by viewModel.hermesVoiceSession.userTranscript.collectAsStateWithLifecycle()
    val hermesResponse by viewModel.hermesVoiceSession.assistantResponse.collectAsStateWithLifecycle()
    val isListening = hermesState == HermesVoiceSession.State.LISTENING ||
            hermesState == HermesVoiceSession.State.PROCESSING ||
            hermesState == HermesVoiceSession.State.SPEAKING

    var showSetup by remember { mutableStateOf(viewModel.needsOnboarding) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (viewModel.hermesVoiceSession.state.value == HermesVoiceSession.State.DISCONNECTED) {
                viewModel.hermesVoiceSession.configure(room = "HERM")
                viewModel.hermesVoiceSession.connect()
            }
            viewModel.hermesVoiceSession.startListening()
        }
    }

    if (showSetup) {
        SetupWizard(
            onComplete = { url, room, token ->
                viewModel.completeOnboarding("$url|$room|$token")
                showSetup = false
            }
        )
    } else {
        // Main voice screen
        Box(modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Hermes Assistant", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)

                Text(hermesStatus, fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                if (hermesTranscript.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)) {
                        Text(hermesTranscript, modifier = Modifier.padding(16.dp), color = OpenRockyPalette.text, fontSize = 16.sp)
                    }
                }

                if (hermesResponse.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                        Text(hermesResponse, modifier = Modifier.padding(16.dp), color = OpenRockyPalette.muted, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isListening) viewModel.hermesVoiceSession.stop()
                        else {
                            val perm = android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == perm) {
                                if (viewModel.hermesVoiceSession.state.value == HermesVoiceSession.State.DISCONNECTED) {
                                    viewModel.hermesVoiceSession.configure(room = "HERM")
                                    viewModel.hermesVoiceSession.connect()
                                }
                                viewModel.hermesVoiceSession.startListening()
                            } else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) OpenRockyPalette.error else OpenRockyPalette.accent
                    )
                ) {
                    Text(if (isListening) "Stop" else "Mic", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SetupWizard(onComplete: (url: String, room: String, token: String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var relayUrl by remember { mutableStateOf("wss://hospitality-musicians-hunting-wedding.trycloudflare.com") }
    var roomCode by remember { mutableStateOf("HERM") }
    var token by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background).systemBarsPadding().padding(32.dp)) {
        AnimatedContent(targetState = step, label = "wizard") { currentStep ->
            when (currentStep) {
                0 -> {
                    // Step 1: Welcome
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(OpenRockyPalette.accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Mic, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Hermes Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text("Voice-activated AI from your desktop.\nConnect once, talk anytime.", fontSize = 15.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(40.dp))

                        OutlineBox("1. Start the relay on your desktop", "Run start-relay.bat")
                        Spacer(Modifier.height(12.dp))
                        OutlineBox("2. Enter the tunnel URL", "Copy from the terminal")
                        Spacer(Modifier.height(12.dp))
                        OutlineBox("3. Enter the shared secret", "Keep this private")
                        Spacer(Modifier.height(12.dp))
                        OutlineBox("4. Tap Connect & start talking", "Voice-to-voice with Hermes")

                        Spacer(Modifier.weight(1f))
                        Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Get Started", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                1 -> {
                    // Step 2: Relay URL
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Step 1 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Relay Connection", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text("Paste the URL from start-relay.bat on your desktop.", fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(value = relayUrl, onValueChange = { relayUrl = it },
                            label = { Text("Relay URL") }, placeholder = { Text("wss://xxx.trycloudflare.com") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            leadingIcon = { Icon(Icons.Default.Cloud, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Already filled in with the current tunnel URL.", fontSize = 11.sp, color = OpenRockyPalette.label)

                        Spacer(Modifier.weight(1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Back") }
                            Button(onClick = { step = 2 }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)) { Text("Next") }
                        }
                    }
                }
                2 -> {
                    // Step 3: Room code + token
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Step 2 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Room & Security", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text("Enter the room code and shared secret from the relay terminal.", fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(value = roomCode, onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                            label = { Text("Room Code") }, placeholder = { Text("HERM") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp))

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(value = token, onValueChange = { token = it },
                            label = { Text("Shared Secret") }, placeholder = { Text("hex token from terminal") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp))

                        Spacer(Modifier.weight(1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Back") }
                            Button(onClick = { step = 3 }, modifier = Modifier.weight(1f), enabled = roomCode.length >= 3,
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)) { Text("Next") }
                        }
                    }
                }
                3 -> {
                    // Step 4: Review & connect
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Step 3 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Ready to Connect", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(16.dp))

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ReviewRow("Relay", relayUrl)
                                ReviewRow("Room", roomCode)
                                ReviewRow("Secret", if (token.isNotEmpty()) "••••${token.takeLast(4)}" else "(none)")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("✓ Desktop running start-relay.bat", fontSize = 13.sp, color = OpenRockyPalette.success)
                        Text("✓ Desktop client connected to relay", fontSize = 13.sp, color = OpenRockyPalette.success)
                        Text("✓ Hermes Agent running on desktop", fontSize = 13.sp, color = OpenRockyPalette.success)

                        Spacer(Modifier.weight(1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Back") }
                            Button(onClick = { onComplete(relayUrl, roomCode, token) }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)) {
                                Text("Connect", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlineBox(title: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = OpenRockyPalette.cardElevated, shape = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(OpenRockyPalette.accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Text(title.first().toString(), color = OpenRockyPalette.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                Text(subtitle, fontSize = 12.sp, color = OpenRockyPalette.muted)
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = OpenRockyPalette.muted)
        Text(value.take(40), fontSize = 13.sp, color = OpenRockyPalette.text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OpenRockyPalette.text,
    unfocusedTextColor = OpenRockyPalette.text,
    cursorColor = OpenRockyPalette.accent,
    focusedBorderColor = OpenRockyPalette.accent,
    unfocusedBorderColor = OpenRockyPalette.stroke,
    focusedContainerColor = OpenRockyPalette.cardElevated,
    unfocusedContainerColor = OpenRockyPalette.card,
)
