//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ChatRoute
@Serializable object SettingsRoute
@Serializable object ProviderInstanceListRoute
@Serializable data class ProviderInstanceEditorRoute(val instanceId: String? = null)
@Serializable object RealtimeProviderInstanceEditorRoute
@Serializable object CharacterSettingsRoute
@Serializable data class CharacterEditorRoute(val characterId: String? = null)
@Serializable object SoulSettingsRoute
@Serializable data class SoulEditorRoute(val soulId: String? = null)
@Serializable object SkillsSettingsRoute
@Serializable object CustomSkillsListRoute
@Serializable data class CustomSkillEditorRoute(val skillId: String? = null)
@Serializable object SkillImportRoute
@Serializable object MCPServersListRoute
@Serializable data class MCPServerEditorRoute(val serverId: String? = null)
@Serializable object MemorySettingsRoute
@Serializable object LogsRoute
@Serializable data class LogFileRoute(val fileName: String)
@Serializable object FeaturesSettingsRoute
@Serializable object UsageSettingsRoute
@Serializable object AboutRoute
@Serializable object WorkspaceFilesRoute
@Serializable data class FilePreviewRoute(val filePath: String)
@Serializable object MountSettingsRoute
@Serializable object EmailSettingsRoute
@Serializable object DebugPanelRoute
@Serializable object OnboardingRoute
@Serializable object ConversationListRoute
@Serializable object SystemIntegrationsRoute
