//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ProviderInstance(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kind: ProviderKind = ProviderKind.OPENAI,
    val modelID: String = "",
    val customHost: String = "",
    val azureResourceName: String = "",
    val azureAPIVersion: String = "2024-02-15-preview",
    val aiProxyServiceURL: String = "",
    val openRouterReferer: String = "",
    val openRouterTitle: String = ""
) {
    val credentialKeychainKey: String
        get() = "provider_credential_$id"

    fun toConfiguration(credential: String): ProviderConfiguration = ProviderConfiguration(
        provider = kind,
        modelID = modelID.ifBlank { kind.defaultModel },
        credential = credential,
        customHost = customHost,
        azureResourceName = azureResourceName,
        azureAPIVersion = azureAPIVersion,
        aiProxyServiceURL = aiProxyServiceURL,
        openRouterReferer = openRouterReferer,
        openRouterTitle = openRouterTitle
    )
}
