package com.xnu.rocky

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    var showOnboarding by remember { mutableStateOf(viewModel.needsOnboarding) }
    var relayUrl by remember { mutableStateOf("wss://hospitality-musicians-hunting-wedding.trycloudflare.com") }
    var roomCode by remember { mutableStateOf("HERM") }
    var token by remember { mutableStateOf("") }

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

    if (showOnboarding) {
        // Simple onboarding — relay connection
        Box(
            modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background).padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Hermes Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                Text("Connect to your desktop Hermes agent", fontSize = 16.sp, color = OpenRockyPalette.muted)

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = relayUrl, onValueChange = { relayUrl = it },
                    label = { Text("Relay URL") },
                    placeholder = { Text("wss://xxx.trycloudflare.com") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = roomCode, onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                    label = { Text("Room Code") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it },
                    label = { Text("Shared Secret") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                Button(
                    onClick = {
                        val url = relayUrl.ifBlank { "wss://hospitality-musicians-hunting-wedding.trycloudflare.com" }
                        viewModel.completeOnboarding("$url|$roomCode|$token")
                        showOnboarding = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = relayUrl.isNotBlank() || true
                ) {
                    Text("Connect")
                }
            }
        }
    } else {
        // Voice home screen
        Box(modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Hermes Assistant", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)

                // Status
                Text(hermesStatus, fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                // Transcript
                if (hermesTranscript.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)) {
                        Text(hermesTranscript, modifier = Modifier.padding(16.dp), color = OpenRockyPalette.text, fontSize = 16.sp)
                    }
                }

                // Response
                if (hermesResponse.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)) {
                        Text(hermesResponse, modifier = Modifier.padding(16.dp), color = OpenRockyPalette.muted, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Mic button
                Button(
                    onClick = {
                        if (isListening) {
                            viewModel.hermesVoiceSession.stop()
                        } else if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            if (viewModel.hermesVoiceSession.state.value == HermesVoiceSession.State.DISCONNECTED) {
                                viewModel.hermesVoiceSession.configure(room = "HERM")
                                viewModel.hermesVoiceSession.connect()
                            }
                            viewModel.hermesVoiceSession.startListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
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
