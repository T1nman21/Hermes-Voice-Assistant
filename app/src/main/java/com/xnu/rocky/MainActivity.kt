package com.xnu.rocky

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.xnu.rocky.hermes.HermesVoiceSession
import com.xnu.rocky.ui.theme.OpenRockyPalette
import com.xnu.rocky.ui.theme.OpenRockyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenRockyTheme {
                HermesMainApp(
                    autoStartVoice = intent?.action == "com.xnu.rocky.action.START_VOICE"
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Re-set content to handle new intent (e.g. wake word fired while app was open)
        setContent {
            OpenRockyTheme {
                HermesMainApp(
                    autoStartVoice = intent.action == "com.xnu.rocky.action.START_VOICE"
                )
            }
        }
    }
}

@Composable
fun HermesMainApp(autoStartVoice: Boolean = false) {
    val viewModel: OpenRockyViewModel = viewModel()
    val context = LocalContext.current
    val app = context.applicationContext as OpenRockyApp

    val hermesState by viewModel.hermesVoiceSession.state.collectAsStateWithLifecycle()
    val hermesStatus by viewModel.hermesVoiceSession.statusText.collectAsStateWithLifecycle()
    val hermesTranscript by viewModel.hermesVoiceSession.userTranscript.collectAsStateWithLifecycle()
    val hermesResponse by viewModel.hermesVoiceSession.assistantResponse.collectAsStateWithLifecycle()
    val isListening = hermesState == HermesVoiceSession.State.LISTENING ||
            hermesState == HermesVoiceSession.State.PROCESSING ||
            hermesState == HermesVoiceSession.State.SPEAKING

    var showSetup by remember { mutableStateOf(viewModel.needsOnboarding) }
    var wakeWordOn by remember { mutableStateOf(app.wakeWordEnabled) }

    // Auto-start voice when triggered by wake word
    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice && !showSetup) {
            val perm = android.content.pm.PackageManager.PERMISSION_GRANTED
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == perm) {
                if (viewModel.hermesVoiceSession.state.value == HermesVoiceSession.State.DISCONNECTED ||
                    viewModel.hermesVoiceSession.state.value == HermesVoiceSession.State.WAITING_FOR_DESKTOP) {
                    // Relay is managed by OpenRockyApp — just start observing if needed
                    viewModel.hermesVoiceSession.startObserving()
                }
                viewModel.hermesVoiceSession.startListening()
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.hermesVoiceSession.startObserving()
            viewModel.hermesVoiceSession.startListening()
        }
    }

    // Battery optimization — prompt during onboarding or first use
    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    LaunchedEffect(Unit) {
        if (!showSetup && app.wakeWordEnabled) {
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                // We don't block the user, but we could show a banner
            }
        }
    }

    if (showSetup) {
        SetupWizard(
            onComplete = { url, room, token ->
                viewModel.completeOnboarding("$url|$room|$token")
                showSetup = false
            },
            onBatteryExemption = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                batteryLauncher.launch(intent)
            }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Wake word toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Always listening", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = OpenRockyPalette.text)
                        Text(
                            "Hey Hermes" + if (wakeWordOn && app.getPicovoiceKey().isBlank()) " (needs Picovoice key)" else "",
                            fontSize = 12.sp,
                            color = OpenRockyPalette.muted
                        )
                    }
                    Switch(
                        checked = wakeWordOn,
                        onCheckedChange = { enabled ->
                            wakeWordOn = enabled
                            viewModel.toggleWakeWord(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OpenRockyPalette.accent,
                            checkedTrackColor = OpenRockyPalette.accent.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text("Hermes Assistant", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)

                Text(hermesStatus, fontSize = 14.sp, color = OpenRockyPalette.muted, textAlign = TextAlign.Center)

                if (hermesTranscript.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.cardElevated)
                    ) {
                        Text(
                            hermesTranscript,
                            modifier = Modifier.padding(16.dp),
                            color = OpenRockyPalette.text,
                            fontSize = 16.sp
                        )
                    }
                }

                if (hermesResponse.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = OpenRockyPalette.card)
                    ) {
                        Text(
                            hermesResponse,
                            modifier = Modifier.padding(16.dp),
                            color = OpenRockyPalette.muted,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isListening) {
                            viewModel.hermesVoiceSession.stop()
                        } else {
                            val perm = android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == perm) {
                                viewModel.hermesVoiceSession.startObserving()
                                viewModel.hermesVoiceSession.startListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
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

                // State indicator
                Text(
                    when (hermesState) {
                        HermesVoiceSession.State.DISCONNECTED -> "⚫ Disconnected"
                        HermesVoiceSession.State.WAITING_FOR_DESKTOP -> "🟡 Waiting for PC"
                        HermesVoiceSession.State.READY -> "🟢 Ready"
                        HermesVoiceSession.State.LISTENING -> "🔴 Listening"
                        HermesVoiceSession.State.PROCESSING -> "🟣 Thinking"
                        HermesVoiceSession.State.SPEAKING -> "🔵 Speaking"
                    },
                    fontSize = 12.sp,
                    color = OpenRockyPalette.label
                )
            }
        }
    }
}

@Composable
fun SetupWizard(
    onComplete: (url: String, room: String, token: String) -> Unit,
    onBatteryExemption: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var relayUrl by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("HERM") }
    var token by remember { mutableStateOf("") }
    var qrError by remember { mutableStateOf<String?>(null) }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { data ->
            val parts = data.split("|")
            if (parts.size >= 3) {
                val url = parts[0]
                val room = parts[1]
                val tok = parts.drop(2).joinToString("|")
                onComplete(url, room, tok)
            } else if (parts.size == 2) {
                relayUrl = parts[0]
                roomCode = parts[1]
                step = 2
            } else {
                relayUrl = data
                step = 1
            }
        }
    }

    fun launchQrScanner() {
        qrError = null
        qrLauncher.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Point camera at the QR code in the Relay window")
            setBeepEnabled(false)
            setOrientationLocked(false)
        })
    }

    Box(
        modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background)
            .systemBarsPadding().padding(32.dp)
    ) {
        AnimatedContent(targetState = step, label = "wizard") { currentStep ->
            when (currentStep) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(OpenRockyPalette.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Hermes Assistant", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Voice-activated AI from your desktop.\nScan the QR code or enter details manually.",
                            fontSize = 15.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { launchQrScanner() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = OpenRockyPalette.background, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Scan QR Code", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (qrError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(qrError!!, fontSize = 13.sp, color = OpenRockyPalette.error)
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = OpenRockyPalette.stroke)
                            Text("  or  ", fontSize = 13.sp, color = OpenRockyPalette.label)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = OpenRockyPalette.stroke)
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Enter Details Manually", fontSize = 15.sp)
                        }

                        Spacer(Modifier.weight(1f))

                        OutlineBox("1. Run the Hermes relay on your desktop", "hermes voice-relay start")
                        Spacer(Modifier.height(12.dp))
                        OutlineBox("2. Scan the QR code in the terminal", "Auto-fills everything — fastest")
                        Spacer(Modifier.height(12.dp))
                        OutlineBox("3. Allow battery exemption", "Needed for always-on 'Hey Hermes'")
                    }
                }
                1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Step 1 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Relay Connection", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Paste the relay URL from the terminal window.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = relayUrl, onValueChange = { relayUrl = it },
                            label = { Text("Relay URL") },
                            placeholder = { Text("ws://192.168.1.x:8643") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            leadingIcon = { Icon(Icons.Default.Cloud, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tip: Scan the QR code instead — it fills this automatically.",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.label
                        )

                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 0 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Back") }
                            Button(
                                onClick = { step = 2 }, modifier = Modifier.weight(1f),
                                enabled = relayUrl.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Next") }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Step 2 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Room & Security", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enter the room code and shared secret from the relay terminal.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value = roomCode,
                            onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                            label = { Text("Room Code") },
                            placeholder = { Text("HERM") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            leadingIcon = { Icon(Icons.Default.MeetingRoom, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = token, onValueChange = { token = it },
                            label = { Text("Shared Secret") },
                            placeholder = { Text("hex token from terminal") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = OpenRockyPalette.accent) },
                            colors = fieldColors(), shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Back") }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f),
                                enabled = roomCode.length >= 3,
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Next") }
                        }
                    }
                }
                3 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Step 3 of 3", fontSize = 13.sp, color = OpenRockyPalette.label)
                        Spacer(Modifier.height(8.dp))
                        Text("Battery Optimization", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OpenRockyPalette.text)
                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                .background(OpenRockyPalette.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BatterySaver, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(12.dp))

                        Text(
                            "For 'Hey Hermes' to work when your phone is locked or in another app, Android requires a battery optimization exemption.\n\nYou can skip this — but then 'Hey Hermes' only works while the app is open.",
                            fontSize = 14.sp,
                            color = OpenRockyPalette.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = onBatteryExemption,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Allow Background Listening", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }

                        Spacer(Modifier.height(12.dp))

                        TextButton(onClick = {
                            onComplete(relayUrl, roomCode, token)
                        }) {
                            Text("Skip (tap-to-talk only)", fontSize = 14.sp, color = OpenRockyPalette.muted)
                        }

                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Back") }
                            Button(
                                onClick = { onComplete(relayUrl, roomCode, token) },
                                modifier = Modifier.weight(1f),
                                enabled = relayUrl.isNotBlank() && roomCode.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = OpenRockyPalette.accent),
                                shape = RoundedCornerShape(14.dp)
                            ) {
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = OpenRockyPalette.cardElevated,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(OpenRockyPalette.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title.first().toString(),
                    color = OpenRockyPalette.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
