//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

data class ProviderConfiguration(
    val provider: ProviderKind,
    val modelID: String,
    val credential: String,
    val customHost: String = "",
    val azureResourceName: String = "",
    val azureAPIVersion: String = "2024-02-15-preview",
    val aiProxyServiceURL: String = "",
    val openRouterReferer: String = "",
    val openRouterTitle: String = ""
) {
    val isValid: Boolean
        get() = credential.isNotBlank() && modelID.isNotBlank()

    val maskedCredential: String
        get() {
            if (credential.length <= 8) return "••••••••"
            return credential.take(4) + "••••" + credential.takeLast(4)
        }

    fun normalized(): ProviderConfiguration = copy(
        credential = credential.trim(),
        modelID = modelID.trim(),
        azureResourceName = azureResourceName.trim(),
        aiProxyServiceURL = aiProxyServiceURL.trim(),
        customHost = customHost.trim().trimEnd('/')
    )
}
