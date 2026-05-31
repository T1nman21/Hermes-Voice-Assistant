//
// OpenRocky — Voice-first AI Agent
// https://github.com/openrocky
//
// Developed by everettjf with the assistance of Claude Code and Codex.
// Date: 2026-03-25
// Copyright (c) 2026 everettjf. All rights reserved.
//

package com.xnu.rocky.providers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object OpenAIServiceFactory {
    fun createClient(config: ProviderConfiguration): OkHttpClient {
        val normalized = config.normalized()
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                val resolvedCredential = resolveCredentialForRequest(normalized)

                when (normalized.provider) {
                    ProviderKind.AZURE_OPENAI -> {
                        builder.header("api-key", resolvedCredential)
                        builder.header("Content-Type", "application/json")
                    }
                    ProviderKind.ANTHROPIC -> {
                        builder.header("x-api-key", resolvedCredential)
                        builder.header("anthropic-version", "2023-06-01")
                        builder.header("Content-Type", "application/json")
                    }
                    ProviderKind.OPENROUTER -> {
                        builder.header("Authorization", "Bearer $resolvedCredential")
                        builder.header("Content-Type", "application/json")
                        if (normalized.openRouterReferer.isNotBlank()) {
                            builder.header("HTTP-Referer", normalized.openRouterReferer)
                        }
                        if (normalized.openRouterTitle.isNotBlank()) {
                            builder.header("X-Title", normalized.openRouterTitle)
                        }
                    }
                    else -> {
                        builder.header("Authorization", "Bearer $resolvedCredential")
                        builder.header("Content-Type", "application/json")
                    }
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    private fun resolveCredentialForRequest(config: ProviderConfiguration): String {
        if (config.provider != ProviderKind.OPENAI) {
            return config.credential
        }
        return try {
            runBlocking { OpenAIOAuthVault.resolvedAccessToken(config.credential) }
        } catch (_: Exception) {
            config.credential
        }
    }

    fun chatCompletionUrl(config: ProviderConfiguration): String {
        val normalized = config.normalized()
        // If custom host is set, use it directly as base URL
        if (normalized.customHost.isNotBlank()) {
            val base = normalized.customHost.trimEnd('/')
            return when (normalized.provider) {
                ProviderKind.ANTHROPIC -> "$base/messages"
                ProviderKind.GEMINI -> "$base/models/${normalized.modelID}:streamGenerateContent?key=${normalized.credential}"
                else -> "$base/chat/completions"
            }
        }

        return when (normalized.provider) {
            ProviderKind.AZURE_OPENAI -> {
                "https://${normalized.azureResourceName}.openai.azure.com/openai/deployments/${normalized.modelID}/chat/completions?api-version=${normalized.azureAPIVersion}"
            }
            ProviderKind.ANTHROPIC -> {
                "${normalized.provider.baseUrl}messages"
            }
            ProviderKind.GEMINI -> {
                "${normalized.provider.baseUrl}models/${normalized.modelID}:streamGenerateContent?key=${normalized.credential}"
            }
            ProviderKind.AIPROXY -> {
                "${normalized.aiProxyServiceURL}/v1/chat/completions"
            }
            else -> {
                "${normalized.provider.baseUrl}chat/completions"
            }
        }
    }
}
