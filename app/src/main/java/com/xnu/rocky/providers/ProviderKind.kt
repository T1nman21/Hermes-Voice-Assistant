package com.xnu.rocky.providers

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderKind(
    val displayName: String,
    val summary: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val apiKeyPlaceholder: String,
    val guideUrl: String,
    val baseUrl: String
) {
    HERMES(
        displayName = "Hermes",
        summary = "Self-hosted — runs on your desktop",
        defaultModel = "hermes",
        suggestedModels = listOf("hermes"),
        apiKeyPlaceholder = "hermes-local",
        guideUrl = "http://localhost:8642/v1",
        baseUrl = "http://localhost:8642/v1/"
    );
}
