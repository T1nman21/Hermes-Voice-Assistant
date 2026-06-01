//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.xnu.rocky.runtime.PermissionHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xnu.rocky.ui.navigation.*
import com.xnu.rocky.ui.screens.chat.*
import com.xnu.rocky.ui.screens.providers.*
import com.xnu.rocky.ui.screens.settings.*
import com.xnu.rocky.ui.theme.OpenRockyTheme
import com.xnu.rocky.ui.theme.OpenRockyPalette
import kotlinx.coroutines.launch

/**
 * External entry-point actions. Rocky exposes these as explicit manifest intent-filters so
 * automation apps (Tasker, Automate, MacroDroid, Home Assistant) and system surfaces (NFC tags,
 * Quick Settings tile, launcher shortcuts, assist button, share sheet) can all drive Rocky with
 * one consistent contract. Example — dispatch from adb or Tasker:
 *
 *     adb shell am start -a com.xnu.rocky.action.SEND_PROMPT \
 *         -e prompt "Summarize my calendar today" \
 *         -n com.xnu.rocky/.MainActivity
 *
 * This Android-only automation surface has no iOS equivalent.
 */
sealed class LaunchRequest {
    /** Assist / voice-command intent OR the `start_voice` launcher shortcut. Auto-opens voice session. */
    data object StartVoice : LaunchRequest()
    /** Start a fresh chat conversation (shortcut, NFC tag, Tasker). */
    data object NewChat : LaunchRequest()
    /** `continue_last` launcher shortcut. Default behavior — resume most recent conversation. */
    data object ContinueLast : LaunchRequest()
    /** Shared text from another app (ACTION_SEND). Pre-fills the chat composer. */
    data class SharedText(val text: String) : LaunchRequest()
    /** Automation-sent prompt. Treated as if the user typed and sent the text — triggers chat reply. */
    data class SendPrompt(val text: String) : LaunchRequest()
    /** Voice foreground-service notification "Stop" button tapped from the shade / lock screen. */
    data object StopVoice : LaunchRequest()
    /** Open a specific conversation by ID (dynamic launcher shortcut). */
    data class OpenConversation(val id: String) : LaunchRequest()
}

const val ACTION_START_VOICE = "com.xnu.rocky.action.START_VOICE"
const val ACTION_STOP_VOICE = "com.xnu.rocky.action.STOP_VOICE"
const val ACTION_NEW_CHAT = "com.xnu.rocky.action.NEW_CHAT"
const val ACTION_CONTINUE_LAST = "com.xnu.rocky.action.CONTINUE_LAST"
const val ACTION_SEND_PROMPT = "com.xnu.rocky.action.SEND_PROMPT"
const val ACTION_OPEN_CONVERSATION = "com.xnu.rocky.action.OPEN_CONVERSATION"
const val EXTRA_PROMPT = "prompt"
const val EXTRA_CONVERSATION_ID = "conversation_id"

private fun Intent.toLaunchRequest(): LaunchRequest? = when (action) {
    Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND, ACTION_START_VOICE -> LaunchRequest.StartVoice
    ACTION_STOP_VOICE -> LaunchRequest.StopVoice
    ACTION_NEW_CHAT -> LaunchRequest.NewChat
    ACTION_CONTINUE_LAST -> LaunchRequest.ContinueLast
    ACTION_SEND_PROMPT -> {
        val prompt = getStringExtra(EXTRA_PROMPT) ?: getStringExtra(Intent.EXTRA_TEXT)
        if (!prompt.isNullOrBlank()) LaunchRequest.SendPrompt(prompt) else null
    }
    ACTION_OPEN_CONVERSATION -> {
        val id = getStringExtra(EXTRA_CONVERSATION_ID)
        if (!id.isNullOrBlank()) LaunchRequest.OpenConversation(id) else null
    }
    Intent.ACTION_SEND -> {
        val shared = getStringExtra(Intent.EXTRA_TEXT)
            ?: getStringExtra(Intent.EXTRA_SUBJECT)
        if (!shared.isNullOrBlank()) LaunchRequest.SharedText(shared) else null
    }
    else -> null
}

class MainActivity : ComponentActivity() {
    private val pendingLaunchRequest = mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingLaunchRequest.value = intent?.toLaunchRequest()

        setContent {
            OpenRockyTheme {
                OpenRockyMainApp(
                    pendingLaunchRequest = pendingLaunchRequest.value,
                    onLaunchRequestConsumed = { pendingLaunchRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // `launchMode=singleTask` routes repeat invocations (assist long-press, shortcut tap, share) here.
        intent.toLaunchRequest()?.let { pendingLaunchRequest.value = it }
    }
}

@Composable
fun OpenRockyMainApp(
    pendingLaunchRequest: LaunchRequest? = null,
    onLaunchRequestConsumed: () -> Unit = {}
) {
    val viewModel: OpenRockyViewModel = viewModel()
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val session by viewModel.sessionRuntime.session.collectAsStateWithLifecycle()
    val isVoiceActive by viewModel.sessionRuntime.isVoiceActive.collectAsStateWithLifecycle()
    val statusText by viewModel.sessionRuntime.statusText.collectAsStateWithLifecycle()
    val liveDelegateProgress by viewModel.sessionRuntime.liveDelegateProgress.collectAsStateWithLifecycle()
    val conversations by viewModel.storageProvider.conversations.collectAsStateWithLifecycle()
    val providerInstances by viewModel.providerStore.instances.collectAsStateWithLifecycle()
    val activeProviderId by viewModel.providerStore.activeInstanceID.collectAsStateWithLifecycle()
    val realtimeInstances by viewModel.realtimeProviderStore.instances.collectAsStateWithLifecycle()
    val activeRealtimeId by viewModel.realtimeProviderStore.activeInstanceID.collectAsStateWithLifecycle()
    val characters by viewModel.characterStore.characters.collectAsStateWithLifecycle()
    val activeCharacterId by viewModel.characterStore.activeCharacterID.collectAsStateWithLifecycle()
    val souls by viewModel.soulStore.souls.collectAsStateWithLifecycle()
    val activeSoulId by viewModel.soulStore.activeSoulID.collectAsStateWithLifecycle()
    val memoryEntries by viewModel.memoryService.entries.collectAsStateWithLifecycle()
    val skills by viewModel.customSkillStore.skills.collectAsStateWithLifecycle()
    val mcpServers by viewModel.toolbox.mcpStore.servers.collectAsStateWithLifecycle()
    val voiceAutoStartOnLaunch by viewModel.preferences.voiceAutoStartOnLaunch.collectAsStateWithLifecycle()
    val hermesState by viewModel.hermesVoiceSession.state.collectAsStateWithLifecycle()
    val hermesStatus by viewModel.hermesVoiceSession.statusText.collectAsStateWithLifecycle()
    val hermesTranscript by viewModel.hermesVoiceSession.userTranscript.collectAsStateWithLifecycle()
    val hermesResponse by viewModel.hermesVoiceSession.assistantResponse.collectAsStateWithLifecycle()
    val isHermesListening = hermesState == com.xnu.rocky.hermes.HermesVoiceSession.State.LISTENING ||
            hermesState == com.xnu.rocky.hermes.HermesVoiceSession.State.PROCESSING ||
            hermesState == com.xnu.rocky.hermes.HermesVoiceSession.State.SPEAKING

    // Microphone permission launcher for voice sessions
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.sessionRuntime.startVoiceSession()
        }
    }

    var showConversationList by remember { mutableStateOf(false) }
    var currentConversationId by remember { mutableStateOf<String?>(null) }
    val activeRealtimeInstance = realtimeInstances.find { it.id == activeRealtimeId }
    val realtimeConfigured = activeRealtimeInstance != null
    val providerLabel = if (realtimeConfigured) {
        val modelDisplay = activeRealtimeInstance!!.modelID.ifBlank { activeRealtimeInstance.kind.defaultModel }
        "${activeRealtimeInstance.kind.displayName} · $modelDisplay"
    } else {
        "Voice not configured"
    }
    val recentMessages by com.xnu.rocky.ui.screens.voice.rememberRecentMessages(
        storage = viewModel.storageProvider,
        conversationId = currentConversationId
    )

    LaunchedEffect(Unit) {
        if (viewModel.needsOnboarding) {
            navController.navigate(OnboardingRoute)
        } else if (conversations.isEmpty()) {
            currentConversationId = viewModel.sessionRuntime.newConversation()
        } else {
            currentConversationId = conversations.firstOrNull()?.id
            currentConversationId?.let { viewModel.sessionRuntime.setConversation(it) }
        }

        // Auto-enter voice on a normal cold launch when the user opted in (default on).
        // External launches (assist intent / shortcut) drive their own voice start via
        // pendingLaunchRequest, so skip when one is in flight. Gated on a configured
        // provider + an already-granted mic permission so a fresh install never hits an
        // error screen or a surprise permission prompt on launch. Mirrors iOS.
        if (!viewModel.needsOnboarding &&
            pendingLaunchRequest == null &&
            realtimeConfigured &&
            viewModel.preferences.voiceAutoStartOnLaunch.value &&
            PermissionHelper.hasMicrophone(context) &&
            !viewModel.sessionRuntime.isVoiceActive.value
        ) {
            kotlinx.coroutines.delay(800)
            if (!viewModel.sessionRuntime.isVoiceActive.value) {
                viewModel.sessionRuntime.startVoiceSession()
            }
        }
    }

    // External launch entry points: assist intent, launcher shortcuts, share sheet, automation (Tasker/NFC).
    LaunchedEffect(pendingLaunchRequest) {
        val req = pendingLaunchRequest ?: return@LaunchedEffect
        when (req) {
            is LaunchRequest.StartVoice -> {
                if (PermissionHelper.hasMicrophone(context)) {
                    viewModel.sessionRuntime.startVoiceSession()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            is LaunchRequest.NewChat -> {
                currentConversationId = viewModel.sessionRuntime.newConversation()
                navController.navigate(ChatRoute)
            }
            is LaunchRequest.ContinueLast -> {
                // Default "resume last conversation" is what the app already does on launch.
                navController.navigate(ChatRoute)
            }
            is LaunchRequest.SharedText -> {
                // Share-sheet text: send immediately on the chat route.
                viewModel.sessionRuntime.sendTextMessage(req.text)
                navController.navigate(ChatRoute)
            }
            is LaunchRequest.SendPrompt -> {
                // Automation-driven prompt: treat as if the user sent it — fires a reply immediately.
                viewModel.sessionRuntime.sendTextMessage(req.text)
                navController.navigate(ChatRoute)
            }
            is LaunchRequest.StopVoice -> {
                // Notification "Stop" button routes here so SessionRuntime (the single source of
                // truth for voice lifecycle) can tear down the bridge + foreground service cleanly.
                viewModel.sessionRuntime.stopVoiceSession()
            }
            is LaunchRequest.OpenConversation -> {
                // Dynamic shortcut or automation — jump straight to the target conversation.
                currentConversationId = req.id
                viewModel.sessionRuntime.setConversation(req.id)
                navController.navigate(ChatRoute)
            }
        }
        onLaunchRequestConsumed()
    }

    // Publish the three most-recent conversations as dynamic launcher shortcuts so long-pressing
    // the Rocky icon surfaces them alongside the static Voice / New Chat / Continue entries.
    LaunchedEffect(conversations) {
        RecentConversationsShortcuts.refresh(context, conversations)
    }

    Box(modifier = Modifier.fillMaxSize().background(OpenRockyPalette.background)) {
        NavHost(navController = navController, startDestination = HomeRoute) {

            composable<HomeRoute> {
                com.xnu.rocky.ui.screens.voice.VoiceHomeScreen(
                    session = session,
                    isVoiceActive = isVoiceActive,
                    realtimeConfigured = realtimeConfigured,
                    providerLabel = providerLabel,
                    recentMessages = recentMessages,
                    liveDelegateProgress = liveDelegateProgress,
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenChat = { navController.navigate(ChatRoute) },
                    onToggleVoice = {
                        if (isHermesListening) {
                            viewModel.hermesVoiceSession.stop()
                        } else if (PermissionHelper.hasMicrophone(context)) {
                            // Auto-connect with defaults if not yet configured
                            if (viewModel.hermesVoiceSession.state.value == com.xnu.rocky.hermes.HermesVoiceSession.State.DISCONNECTED) {
                                viewModel.hermesVoiceSession.configure(room = "HERM")
                                viewModel.hermesVoiceSession.connect()
                            }
                            viewModel.hermesVoiceSession.startListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }

            composable<ChatRoute> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopChromeView(
                        isVoiceActive = isVoiceActive,
                        onSettingsClick = { navController.navigate(SettingsRoute) },
                        onVoiceToggle = {
                            if (isVoiceActive) {
                                viewModel.sessionRuntime.stopVoiceSession()
                            } else {
                                navController.popBackStack(HomeRoute, inclusive = false)
                            }
                        },
                        onNewChatClick = {
                            currentConversationId = viewModel.sessionRuntime.newConversation()
                        },
                        onConversationsClick = { showConversationList = true }
                    )

                    val messages = currentConversationId?.let {
                        viewModel.storageProvider.loadMessages(it)
                    } ?: emptyList()

                    ChatScreen(messages = messages, modifier = Modifier.weight(1f))
                    // Composer is text-only and minimalist — the conversation list lives in the top
                    // chrome on this route, so the composer doesn't need its own Forum button.
                    com.xnu.rocky.ui.screens.home.ComposerBarStandalone(
                        onSendMessage = { viewModel.sessionRuntime.sendTextMessage(it) },
                        onConversationsClick = { showConversationList = true },
                        minimalistLayout = true
                    )
                }
            }

            composable<SettingsRoute> {
                ProviderSettingsView(
                    onBack = { navController.popBackStack() },
                    onChatProviders = { navController.navigate(ProviderInstanceListRoute) },
                    onVoiceProviders = { navController.navigate(RealtimeProviderInstanceEditorRoute) },
                    onCharacters = { navController.navigate(CharacterSettingsRoute) },
                    onSoul = { navController.navigate(SoulSettingsRoute) },
                    onSkills = { navController.navigate(SkillsSettingsRoute) },
                    onCustomSkills = { navController.navigate(CustomSkillsListRoute) },
                    onMCPServers = { navController.navigate(MCPServersListRoute) },
                    onMemory = { navController.navigate(MemorySettingsRoute) },
                    onEmail = { navController.navigate(EmailSettingsRoute) },
                    onFeatures = { navController.navigate(FeaturesSettingsRoute) },
                    onUsage = { navController.navigate(UsageSettingsRoute) },
                    onMounts = { navController.navigate(MountSettingsRoute) },
                    onWorkspace = { navController.navigate(WorkspaceFilesRoute) },
                    onLogs = { navController.navigate(LogsRoute) },
                    onDebug = { navController.navigate(DebugPanelRoute) },
                    onSystemIntegrations = { navController.navigate(SystemIntegrationsRoute) },
                    onAbout = { navController.navigate(AboutRoute) },
                    voiceAutoStartOnLaunch = voiceAutoStartOnLaunch,
                    onVoiceAutoStartChange = { viewModel.preferences.setVoiceAutoStartOnLaunch(it) }
                )
            }

            composable<SystemIntegrationsRoute> {
                com.xnu.rocky.ui.screens.settings.SystemIntegrationsView(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ProviderInstanceListRoute> {
                ProviderInstanceListView(
                    instances = providerInstances,
                    activeInstanceId = activeProviderId,
                    onSelect = { viewModel.providerStore.activate(it) },
                    onEdit = { navController.navigate(ProviderInstanceEditorRoute(instanceId = it)) },
                    onDelete = { viewModel.providerStore.delete(it) },
                    onAdd = { navController.navigate(ProviderInstanceEditorRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<ProviderInstanceEditorRoute> { entry ->
                val route = entry.toRoute<ProviderInstanceEditorRoute>()
                val existing = route.instanceId?.let { id -> providerInstances.find { it.id == id } }
                val credential = existing?.let { viewModel.providerStore.credentialFor(it) } ?: ""
                val existingOAuthCredential = existing?.let { viewModel.providerStore.openAIOAuthCredential(it) }

                ProviderInstanceEditorView(
                    existingInstance = existing,
                    existingCredential = credential,
                    existingOpenAIOAuthCredential = existingOAuthCredential,
                    onSave = { inst, cred, oauthCred ->
                        viewModel.providerStore.save(inst, cred)
                        viewModel.providerStore.setOpenAIOAuthCredential(oauthCred, inst.id)
                    },
                    onTest = { inst, cred, oauthCred, callback ->
                        scope.launch {
                            val refreshedOAuth = if (oauthCred != null && cred.isBlank()) {
                                runCatching { com.xnu.rocky.providers.OpenAIOAuthService.refreshIfNeeded(oauthCred) }.getOrElse { oauthCred }
                            } else {
                                oauthCred
                            }
                            if (existing != null && refreshedOAuth != null && refreshedOAuth != oauthCred) {
                                viewModel.providerStore.setOpenAIOAuthCredential(refreshedOAuth, existing.id)
                            }
                            val resolvedCredential = cred.ifBlank { refreshedOAuth?.accessToken.orEmpty() }
                            val config = inst.toConfiguration(resolvedCredential)
                            val client = com.xnu.rocky.providers.ChatClient(config)
                            val result = client.testConnection()
                            callback(result.getOrElse { it.message ?: "Connection failed" })
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<RealtimeProviderInstanceEditorRoute> {
                RealtimeProviderInstanceEditorView(
                    realtimeProviderStore = viewModel.realtimeProviderStore,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CharacterSettingsRoute> {
                CharacterSettingsView(
                    characters = characters,
                    activeCharacterId = activeCharacterId,
                    onActivate = { viewModel.characterStore.activate(it) },
                    onEdit = { navController.navigate(CharacterEditorRoute(characterId = it)) },
                    onDelete = { viewModel.characterStore.delete(it) },
                    onAdd = { navController.navigate(CharacterEditorRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CharacterEditorRoute> { entry ->
                val route = entry.toRoute<CharacterEditorRoute>()
                val character = route.characterId?.let { id -> characters.find { it.id == id } }
                CharacterEditorView(
                    character = character,
                    onSave = { viewModel.characterStore.save(it) },
                    onReset = if (character?.isBuiltIn == true) { { id -> viewModel.characterStore.resetBuiltIn(id) } } else null,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SoulSettingsRoute> {
                SoulSettingsView(
                    souls = souls,
                    activeSoulId = activeSoulId,
                    onActivate = { viewModel.soulStore.activate(it) },
                    onEdit = { navController.navigate(SoulEditorRoute(soulId = it)) },
                    onDelete = { viewModel.soulStore.delete(it) },
                    onAdd = { navController.navigate(SoulEditorRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SoulEditorRoute> { entry ->
                val route = entry.toRoute<SoulEditorRoute>()
                val soul = route.soulId?.let { id -> souls.find { it.id == id } }
                SoulEditorView(
                    existingSoul = soul,
                    soulStore = viewModel.soulStore,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SkillsSettingsRoute> {
                SkillsSettingsView(
                    toolStore = viewModel.builtInToolStore,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CustomSkillsListRoute> {
                CustomSkillsListView(
                    skills = skills,
                    onToggle = { viewModel.customSkillStore.toggleEnabled(it) },
                    onEdit = { navController.navigate(CustomSkillEditorRoute(skillId = it)) },
                    onDelete = { viewModel.customSkillStore.delete(it) },
                    onAdd = { navController.navigate(CustomSkillEditorRoute()) },
                    onImport = { navController.navigate(SkillImportRoute) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SkillImportRoute> {
                SkillImportView(
                    skillStore = viewModel.customSkillStore,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<CustomSkillEditorRoute> { entry ->
                val route = entry.toRoute<CustomSkillEditorRoute>()
                val skill = route.skillId?.let { id -> skills.find { it.id == id } }
                CustomSkillEditorView(
                    skill = skill,
                    onSave = { viewModel.customSkillStore.save(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MCPServersListRoute> {
                com.xnu.rocky.ui.screens.settings.MCPServersListView(
                    servers = mcpServers,
                    onToggle = { viewModel.toolbox.mcpStore.toggle(it) },
                    onEdit = { navController.navigate(MCPServerEditorRoute(serverId = it)) },
                    onDelete = { viewModel.toolbox.mcpStore.delete(it) },
                    onAdd = { navController.navigate(MCPServerEditorRoute()) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MCPServerEditorRoute> { entry ->
                val route = entry.toRoute<MCPServerEditorRoute>()
                val existing = route.serverId?.let { id -> mcpServers.find { it.id == id } }
                com.xnu.rocky.ui.screens.settings.MCPServerEditorView(
                    existing = existing,
                    onSave = {
                        if (existing == null) viewModel.toolbox.mcpStore.add(it)
                        else viewModel.toolbox.mcpStore.update(it)
                    },
                    onCacheTools = { id, tools -> viewModel.toolbox.mcpStore.updateCachedTools(id, tools) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<MemorySettingsRoute> {
                MemorySettingsView(
                    entries = memoryEntries,
                    onDelete = { viewModel.memoryService.delete(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<LogsRoute> {
                LogsView(
                    onViewFile = { navController.navigate(LogFileRoute(fileName = it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<LogFileRoute> { entry ->
                val route = entry.toRoute<LogFileRoute>()
                LogFileView(filePath = route.fileName, onBack = { navController.popBackStack() })
            }

            composable<FeaturesSettingsRoute> { FeaturesSettingsView(onBack = { navController.popBackStack() }) }
            composable<UsageSettingsRoute> { UsageSettingsView(usageService = viewModel.usageService, onBack = { navController.popBackStack() }) }
            composable<AboutRoute> { AboutView(onBack = { navController.popBackStack() }) }

            composable<MountSettingsRoute> {
                MountSettingsView(
                    mountStore = viewModel.toolbox.mountStore,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<EmailSettingsRoute> {
                EmailSettingsView(onBack = { navController.popBackStack() })
            }

            composable<DebugPanelRoute> {
                val chatStatus = viewModel.providerStore.let { store ->
                    val inst = store.instances.value.find { it.id == store.activeInstanceID.value }
                    com.xnu.rocky.models.ProviderStatus(
                        name = inst?.kind?.displayName ?: "None",
                        model = inst?.modelID ?: "",
                        isConnected = inst != null
                    )
                }
                val voiceStatus = viewModel.realtimeProviderStore.let { store ->
                    val inst = store.instances.value.find { it.id == store.activeInstanceID.value }
                    com.xnu.rocky.models.ProviderStatus(
                        name = inst?.kind?.displayName ?: "None",
                        model = inst?.modelID ?: "",
                        isConnected = inst != null
                    )
                }
                DebugPanelView(
                    session = session,
                    chatProviderStatus = chatStatus,
                    voiceProviderStatus = voiceStatus,
                    toolCount = viewModel.toolbox.chatToolDefinitions().size,
                    skillCount = skills.count { it.enabled },
                    memoryCount = memoryEntries.size,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<WorkspaceFilesRoute> {
                WorkspaceFilesView(
                    context = context,
                    onNavigate = { },
                    onPreview = { navController.navigate(FilePreviewRoute(filePath = it)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<FilePreviewRoute> { entry ->
                val route = entry.toRoute<FilePreviewRoute>()
                FilePreviewView(filePath = route.filePath, onBack = { navController.popBackStack() })
            }

            composable<OnboardingRoute> {
                OnboardingView(
                    onComplete = { apiKey ->
                        viewModel.completeOnboarding(apiKey)
                        navController.popBackStack()
                        currentConversationId = viewModel.sessionRuntime.newConversation()
                    },
                    onSkip = {
                        viewModel.completeOnboarding("")
                        navController.popBackStack()
                        currentConversationId = viewModel.sessionRuntime.newConversation()
                    }
                )
            }
        }

        // Conversation list sheet overlay
        if (showConversationList) {
            ConversationListView(
                conversations = conversations,
                activeConversationId = currentConversationId,
                onSelect = { id ->
                    currentConversationId = id
                    viewModel.sessionRuntime.setConversation(id)
                    showConversationList = false
                },
                onDelete = { viewModel.storageProvider.deleteConversation(it) },
                onNewConversation = {
                    currentConversationId = viewModel.sessionRuntime.newConversation()
                    showConversationList = false
                },
                onDismiss = { showConversationList = false }
            )
        }

    }
}
