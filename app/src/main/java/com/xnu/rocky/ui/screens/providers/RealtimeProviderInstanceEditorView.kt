//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.screens.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnu.rocky.providers.*
import com.xnu.rocky.runtime.voice.OpenAIVoice
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private sealed class TestState {
    data object Idle : TestState()
    data object Testing : TestState()
    data class Success(val detail: String) : TestState()
    data class Failure(val message: String) : TestState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeProviderInstanceEditorView(
    realtimeProviderStore: RealtimeProviderStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val activeInstance = remember { realtimeProviderStore.activeInstance ?: RealtimeProviderInstance() }

    var credential by remember { mutableStateOf(realtimeProviderStore.credentialFor(activeInstance)) }
    var openaiVoice by remember { mutableStateOf(activeInstance.openaiVoice.ifBlank { "alloy" }) }
    var customHost by remember { mutableStateOf(activeInstance.customHost) }
    var advanced by remember { mutableStateOf(activeInstance.effectiveAdvancedSettings) }
    var showPassword by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var testState by remember { mutableStateOf<TestState>(TestState.Idle) }
    val voicePreview = rememberOpenAIVoicePreview()

    val draftConfigured = credential.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Realtime Voice", color = OpenRockyPalette.text) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OpenRockyPalette.text)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val updated = activeInstance.copy(
                                kind = RealtimeProviderKind.OPENAI,
                                modelID = advanced.realtimeModel,
                                openaiVoice = openaiVoice,
                                customHost = customHost,
                                advancedSettings = if (advanced == RealtimeAdvancedSettings.DEFAULT) null else advanced
                            )
                            realtimeProviderStore.save(updated, credential.trim())
                            onBack()
                        },
                        enabled = draftConfigured
                    ) {
                        Text("Save", color = if (draftConfigured) OpenRockyPalette.accent else OpenRockyPalette.label)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OpenRockyPalette.background)
            )
        },
        containerColor = OpenRockyPalette.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                ProviderHeader()
            }
            item {
                CredentialSection(
                    credential = credential,
                    onCredentialChange = { credential = it; testState = TestState.Idle },
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword },
                    onOpenGuide = { uriHandler.openUri(RealtimeProviderKind.OPENAI.guideUrl) }
                )
            }
            item {
                VoiceSection(
                    selectedVoice = openaiVoice,
                    onSelect = { openaiVoice = it },
                    onPreview = { voice ->
                        voicePreview.toggle(voice, credential.trim(), customHost.trim())
                    },
                    playingVoice = voicePreview.playingVoice,
                    previewLoading = voicePreview.loading,
                    previewError = voicePreview.error
                )
            }
            item {
                CustomHostSection(value = customHost, onChange = { customHost = it })
            }
            item {
                AdvancedSettingsSection(
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded },
                    settings = advanced,
                    onChange = { advanced = it }
                )
            }
            item {
                TestConnectionSection(
                    state = testState,
                    enabled = draftConfigured && testState !is TestState.Testing,
                    onRun = {
                        testState = TestState.Testing
                        scope.launch {
                            val testModel = "gpt-realtime-2"
                            val result = runTestConnection(credential.trim(), customHost.trim(), testModel)
                            testState = result
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            tint = OpenRockyPalette.secondary,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("OpenAI Realtime", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.text)
            Text(
                RealtimeProviderKind.OPENAI.summary,
                fontSize = 12.sp,
                color = OpenRockyPalette.muted,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CredentialSection(
    credential: String,
    onCredentialChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("API Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
        OutlinedTextField(
            value = credential,
            onValueChange = onCredentialChange,
            placeholder = { Text(RealtimeProviderKind.OPENAI.credentialPlaceholder, color = OpenRockyPalette.label) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        tint = OpenRockyPalette.muted
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = rockyTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Row(
            modifier = Modifier.clickable(onClick = onOpenGuide),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = OpenRockyPalette.accent, modifier = Modifier.size(14.dp))
            Text("Get OpenAI API Key", fontSize = 12.sp, color = OpenRockyPalette.accent)
        }
    }
}

@Composable
private fun VoiceSection(
    selectedVoice: String,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    playingVoice: String?,
    previewLoading: Boolean,
    previewError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Voice", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OpenAIVoice.entries.forEach { voice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OpenRockyPalette.cardElevated)
                        .clickable { onSelect(voice.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (selectedVoice == voice.id) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (selectedVoice == voice.id) OpenRockyPalette.accent else OpenRockyPalette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        voice.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = OpenRockyPalette.text,
                        modifier = Modifier.weight(1f)
                    )
                    val isPlaying = playingVoice == voice.id
                    val isLoading = isPlaying && previewLoading
                    IconButton(
                        onClick = { onPreview(voice.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        when {
                            isLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = OpenRockyPalette.accent
                            )
                            isPlaying -> Icon(
                                Icons.Default.StopCircle,
                                "Stop preview",
                                tint = OpenRockyPalette.warning,
                                modifier = Modifier.size(22.dp)
                            )
                            else -> Icon(
                                Icons.Default.PlayCircle,
                                "Play preview",
                                tint = OpenRockyPalette.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        if (!previewError.isNullOrBlank()) {
            Text(previewError, fontSize = 11.sp, color = OpenRockyPalette.error)
        }
    }
}

@Composable
private fun CustomHostSection(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Custom Host (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OpenRockyPalette.muted)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("wss://your-proxy.example.com", color = OpenRockyPalette.label) },
            modifier = Modifier.fillMaxWidth(),
            colors = rockyTextFieldColors(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        Text(
            "Override the default WebSocket host (for proxies / self-hosting).",
            fontSize = 11.sp,
            color = OpenRockyPalette.label
        )
    }
}

@Composable
private fun AdvancedSettingsSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    settings: RealtimeAdvancedSettings,
    onChange: (RealtimeAdvancedSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OpenRockyPalette.cardElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Tune, null, tint = OpenRockyPalette.muted, modifier = Modifier.size(18.dp))
            Text(
                "Advanced Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OpenRockyPalette.text,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = OpenRockyPalette.muted
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChoiceRow(
                    label = "Realtime Model",
                    options = RealtimeAdvancedSettings.realtimeModelOptions,
                    selected = settings.realtimeModel,
                    onSelect = { onChange(settings.copy(realtimeModel = it)) }
                )
                ReasoningEffortRow(
                    selected = settings.reasoningEffort,
                    onSelect = { onChange(settings.copy(reasoningEffort = it)) }
                )
                ChoiceRow(
                    label = "Transcription Model",
                    options = RealtimeAdvancedSettings.transcriptionModelOptions,
                    selected = settings.transcriptionModel,
                    onSelect = { onChange(settings.copy(transcriptionModel = it)) }
                )
                FieldLabel("Input Language (optional, e.g. zh, en)")
                OutlinedTextField(
                    value = settings.inputLanguage ?: "",
                    onValueChange = { onChange(settings.copy(inputLanguage = it.ifBlank { null })) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = rockyTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                TurnDetectionEditor(
                    value = settings.turnDetection,
                    onChange = { onChange(settings.copy(turnDetection = it)) }
                )

                SliderField(
                    label = "Max Output Tokens",
                    value = settings.maxOutputTokens.toFloat(),
                    range = 256f..4096f,
                    valueLabel = "${settings.maxOutputTokens}",
                    onChange = { onChange(settings.copy(maxOutputTokens = it.toInt())) }
                )
                SliderField(
                    label = "Speed",
                    value = settings.speed.toFloat(),
                    range = 0.5f..2.0f,
                    valueLabel = "%.2fx".format(settings.speed),
                    onChange = { onChange(settings.copy(speed = it.toDouble())) }
                )
                SliderField(
                    label = "Barge-in RMS Threshold",
                    value = settings.bargeInRMSThreshold.toFloat(),
                    range = 1000f..8000f,
                    valueLabel = "${settings.bargeInRMSThreshold}",
                    onChange = { onChange(settings.copy(bargeInRMSThreshold = it.toInt())) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Text-Only Replies", fontSize = 13.sp, color = OpenRockyPalette.text)
                        Text(
                            "Let the model reply in text without TTS for visual responses.",
                            fontSize = 11.sp,
                            color = OpenRockyPalette.muted
                        )
                    }
                    Switch(
                        checked = settings.allowTextOnly,
                        onCheckedChange = { onChange(settings.copy(allowTextOnly = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnDetectionEditor(value: TurnDetection, onChange: (TurnDetection) -> Unit) {
    val isSemantic = value is TurnDetection.Semantic
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Turn Detection", fontSize = 13.sp, color = OpenRockyPalette.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isSemantic,
                onClick = { onChange(TurnDetection.Semantic()) },
                label = { Text("Semantic", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                    containerColor = OpenRockyPalette.cardElevated
                )
            )
            FilterChip(
                selected = !isSemantic,
                onClick = { onChange(TurnDetection.Server()) },
                label = { Text("Server VAD", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                    containerColor = OpenRockyPalette.cardElevated
                )
            )
        }
        when (value) {
            is TurnDetection.Semantic -> {
                ChoiceRow(
                    label = "Eagerness",
                    options = listOf("low", "auto", "high"),
                    selected = value.eagerness,
                    onSelect = { onChange(TurnDetection.Semantic(eagerness = it)) }
                )
            }
            is TurnDetection.Server -> {
                SliderField(
                    label = "Prefix Padding (ms)",
                    value = value.prefixPaddingMs.toFloat(),
                    range = 0f..1000f,
                    valueLabel = "${value.prefixPaddingMs} ms",
                    onChange = { onChange(value.copy(prefixPaddingMs = it.toInt())) }
                )
                SliderField(
                    label = "Silence Duration (ms)",
                    value = value.silenceDurationMs.toFloat(),
                    range = 100f..2000f,
                    valueLabel = "${value.silenceDurationMs} ms",
                    onChange = { onChange(value.copy(silenceDurationMs = it.toInt())) }
                )
                SliderField(
                    label = "Threshold",
                    value = value.threshold.toFloat(),
                    range = 0f..1f,
                    valueLabel = "%.2f".format(value.threshold),
                    onChange = { onChange(value.copy(threshold = it.toDouble())) }
                )
            }
        }
    }
}

@Composable
private fun ReasoningEffortRow(selected: ReasoningEffort, onSelect: (ReasoningEffort) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Reasoning Effort")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReasoningEffort.entries.forEach { effort ->
                FilterChip(
                    selected = selected == effort,
                    onClick = { onSelect(effort) },
                    label = { Text(effort.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                        containerColor = OpenRockyPalette.cardElevated
                    )
                )
            }
        }
        Text(selected.summary, fontSize = 11.sp, color = OpenRockyPalette.muted)
    }
}

@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OpenRockyPalette.accent.copy(alpha = 0.2f),
                        containerColor = OpenRockyPalette.cardElevated
                    )
                )
            }
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = OpenRockyPalette.text, modifier = Modifier.weight(1f))
            Text(valueLabel, fontSize = 12.sp, color = OpenRockyPalette.muted)
        }
        Slider(value = value, valueRange = range, onValueChange = onChange)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 12.sp, color = OpenRockyPalette.muted)
}

@Composable
private fun TestConnectionSection(
    state: TestState,
    enabled: Boolean,
    onRun: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(
            onClick = onRun,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = OpenRockyPalette.accent,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (state) {
                is TestState.Testing -> {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Testing…")
                }
                else -> {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test Connection")
                }
            }
        }
        when (state) {
            is TestState.Success -> Text(
                "Connected — ${state.detail}",
                fontSize = 12.sp,
                color = OpenRockyPalette.success
            )
            is TestState.Failure -> Text(
                state.message,
                fontSize = 12.sp,
                color = OpenRockyPalette.error
            )
            else -> Text(
                "Opens a WebSocket to verify your credential and model are accepted.",
                fontSize = 11.sp,
                color = OpenRockyPalette.label
            )
        }
    }
}

private suspend fun runTestConnection(
    credential: String,
    customHost: String,
    testModel: String
): TestState = withContext(Dispatchers.IO) {
    if (credential.isBlank()) return@withContext TestState.Failure("Add an API key first.")
    val host = customHost.trim().ifBlank { "wss://api.openai.com" }
        .removeSuffix("/")
    val url = "$host/v1/realtime?model=$testModel"
    val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    // GA-only models like `gpt-realtime-2` reject the legacy
    // `OpenAI-Beta: realtime=v1` header ("Model is only available on the GA
    // API"). Just send the bearer.
    val request = Request.Builder()
        .url(url.replace("ws://", "http://").replace("wss://", "https://"))
        .header("Authorization", "Bearer $credential")
        .build()
    val outcome = kotlin.runCatching {
        withTimeout(10_000) {
            kotlinx.coroutines.suspendCancellableCoroutine<TestState> { cont ->
                val ws: WebSocket = client.newWebSocket(request, object : WebSocketListener() {
                    private var closed = false
                    private fun resume(state: TestState) {
                        if (closed) return
                        closed = true
                        cont.resumeWith(Result.success(state))
                    }
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        // Wait for the first server frame so we surface auth/model errors with their JSON message.
                    }
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val obj = Json.parseToJsonElement(text)
                            val typeStr = obj.toString()
                            if (typeStr.contains("\"type\":\"error\"")) {
                                resume(TestState.Failure(text.take(240)))
                            } else {
                                resume(TestState.Success(testModel))
                            }
                        } catch (_: Exception) {
                            resume(TestState.Success(testModel))
                        } finally {
                            webSocket.cancel()
                        }
                    }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        val body = response?.body?.string()?.take(240).orEmpty()
                        val msg = if (response != null) "HTTP ${response.code} ${body}" else (t.message ?: "connection failed")
                        resume(TestState.Failure(msg))
                    }
                })
                cont.invokeOnCancellation { ws.cancel() }
            }
        }
    }
    outcome.getOrElse { TestState.Failure("Connection timed out") }
}
